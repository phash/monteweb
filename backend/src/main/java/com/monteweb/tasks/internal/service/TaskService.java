package com.monteweb.tasks.internal.service;

import com.monteweb.room.RoomModuleApi;
import com.monteweb.shared.exception.BadRequestException;
import com.monteweb.shared.exception.ForbiddenException;
import com.monteweb.shared.exception.ResourceNotFoundException;
import com.monteweb.tasks.TaskDeletedEvent;
import com.monteweb.tasks.TaskSavedEvent;
import com.monteweb.tasks.TasksModuleApi;
import com.monteweb.tasks.internal.dto.*;
import com.monteweb.tasks.internal.model.ChecklistItem;
import com.monteweb.tasks.internal.model.Task;
import com.monteweb.tasks.internal.model.TaskBoard;
import com.monteweb.tasks.internal.model.TaskColumn;
import com.monteweb.tasks.internal.repository.ChecklistItemRepository;
import com.monteweb.tasks.internal.repository.TaskBoardRepository;
import com.monteweb.tasks.internal.repository.TaskColumnRepository;
import com.monteweb.tasks.internal.repository.TaskRepository;
import com.monteweb.user.UserInfo;
import com.monteweb.user.UserModuleApi;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@ConditionalOnProperty(prefix = "monteweb.modules", name = "tasks.enabled", havingValue = "true")
@RequiredArgsConstructor
public class TaskService implements TasksModuleApi {

    private final TaskBoardRepository boardRepo;
    private final TaskColumnRepository columnRepo;
    private final TaskRepository taskRepo;
    private final ChecklistItemRepository checklistRepo;
    private final UserModuleApi userModule;
    private final RoomModuleApi roomModule;
    private final ApplicationEventPublisher eventPublisher;

    // ---- Board ----

    /**
     * Gets existing board or creates one with 3 default columns.
     */
    @Transactional
    public TaskBoard getOrCreateBoard(UUID roomId) {
        return boardRepo.findByRoomId(roomId).orElseGet(() -> {
            var board = new TaskBoard();
            board.setRoomId(roomId);
            boardRepo.save(board);

            // Create default columns
            createDefaultColumn(board.getId(), "Offen", 0);
            createDefaultColumn(board.getId(), "In Arbeit", 1);
            createDefaultColumn(board.getId(), "Erledigt", 2);

            return board;
        });
    }

    private void createDefaultColumn(UUID boardId, String name, int position) {
        var col = new TaskColumn();
        col.setBoardId(boardId);
        col.setName(name);
        col.setPosition(position);
        columnRepo.save(col);
    }

    /**
     * Returns the full board response with columns and tasks, resolving user names.
     */
    public TaskBoardResponse getBoard(UUID roomId) {
        var board = getOrCreateBoard(roomId);
        var columns = columnRepo.findByBoardIdOrderByPosition(board.getId());
        var tasks = taskRepo.findByBoardId(board.getId());

        // Collect all user IDs for name resolution
        Set<UUID> userIds = new HashSet<>();
        for (var task : tasks) {
            userIds.add(task.getCreatedBy());
            if (task.getAssigneeId() != null) userIds.add(task.getAssigneeId());
        }
        Map<UUID, String> userNames = resolveUserNames(userIds);

        var columnResponses = columns.stream()
                .map(c -> new TaskColumnResponse(c.getId(), c.getName(), c.getPosition()))
                .toList();

        // Load checklist items for all tasks in batch
        List<UUID> taskIds = tasks.stream().map(Task::getId).toList();
        List<ChecklistItem> allChecklistItems = taskIds.isEmpty()
                ? List.of()
                : checklistRepo.findByTaskIdIn(taskIds);
        Map<UUID, List<ChecklistItem>> checklistByTask = allChecklistItems.stream()
                .collect(java.util.stream.Collectors.groupingBy(ChecklistItem::getTaskId));

        var taskResponses = tasks.stream()
                .map(t -> {
                    List<ChecklistItem> items = checklistByTask.getOrDefault(t.getId(), List.of());
                    List<ChecklistItemResponse> itemResponses = items.stream()
                            .sorted(Comparator.comparingInt(ChecklistItem::getPosition))
                            .map(i -> new ChecklistItemResponse(i.getId(), i.getTitle(), i.isChecked(), i.getPosition()))
                            .toList();
                    int total = items.size();
                    int checked = (int) items.stream().filter(ChecklistItem::isChecked).count();
                    return new TaskResponse(
                            t.getId(),
                            t.getColumnId(),
                            t.getTitle(),
                            t.getDescription(),
                            t.getAssigneeId(),
                            t.getAssigneeId() != null ? userNames.getOrDefault(t.getAssigneeId(), "Unbekannt") : null,
                            t.getCreatedBy(),
                            userNames.getOrDefault(t.getCreatedBy(), "Unbekannt"),
                            t.getDueDate(),
                            t.getPosition(),
                            t.getCreatedAt(),
                            itemResponses,
                            total,
                            checked
                    );
                })
                .toList();

        return new TaskBoardResponse(board.getId(), board.getRoomId(), columnResponses, taskResponses);
    }

    // ---- Tasks ----

    @Transactional
    public TaskResponse createTask(UUID roomId, UUID userId, CreateTaskRequest request) {
        requireRoomMembership(userId, roomId);
        var board = getOrCreateBoard(roomId);

        // Verify column belongs to this board
        var column = columnRepo.findById(request.columnId())
                .orElseThrow(() -> new ResourceNotFoundException("Column not found"));
        if (!column.getBoardId().equals(board.getId())) {
            throw new BadRequestException("Column does not belong to this board");
        }

        // Position: append to end of column
        int maxPosition = taskRepo.findByColumnIdOrderByPosition(request.columnId())
                .stream().mapToInt(Task::getPosition).max().orElse(-1);

        var task = new Task();
        task.setBoardId(board.getId());
        task.setColumnId(request.columnId());
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setAssigneeId(request.assigneeId());
        task.setCreatedBy(userId);
        task.setDueDate(request.dueDate());
        task.setPosition(maxPosition + 1);
        taskRepo.save(task);

        publishTaskSaved(task, board.getRoomId());
        return toResponse(task);
    }

    @Transactional
    public TaskResponse updateTask(UUID taskId, UUID userId, UpdateTaskRequest request) {
        var task = requireTask(taskId);
        var board = boardRepo.findById(task.getBoardId())
                .orElseThrow(() -> new ResourceNotFoundException("Board not found"));
        requireRoomMembership(userId, board.getRoomId());

        if (request.title() != null && !request.title().isBlank()) task.setTitle(request.title());
        if (request.description() != null) task.setDescription(request.description());
        if (request.assigneeId() != null) task.setAssigneeId(request.assigneeId());
        if (request.dueDate() != null) task.setDueDate(request.dueDate());
        if (request.columnId() != null) {
            var column = columnRepo.findById(request.columnId())
                    .orElseThrow(() -> new ResourceNotFoundException("Column not found"));
            if (!column.getBoardId().equals(board.getId())) {
                throw new BadRequestException("Column does not belong to this board");
            }
            task.setColumnId(request.columnId());
        }
        if (request.position() != null) task.setPosition(request.position());

        taskRepo.save(task);
        publishTaskSaved(task, board.getRoomId());
        return toResponse(task);
    }

    @Transactional
    public TaskResponse moveTask(UUID taskId, UUID userId, UUID columnId, int position) {
        var task = requireTask(taskId);
        var board = boardRepo.findById(task.getBoardId())
                .orElseThrow(() -> new ResourceNotFoundException("Board not found"));
        requireRoomMembership(userId, board.getRoomId());

        var column = columnRepo.findById(columnId)
                .orElseThrow(() -> new ResourceNotFoundException("Column not found"));
        if (!column.getBoardId().equals(board.getId())) {
            throw new BadRequestException("Column does not belong to this board");
        }

        task.setColumnId(columnId);
        task.setPosition(position);
        taskRepo.save(task);

        return toResponse(task);
    }

    @Transactional
    public void deleteTask(UUID taskId, UUID userId) {
        var task = requireTask(taskId);
        var board = boardRepo.findById(task.getBoardId())
                .orElseThrow(() -> new ResourceNotFoundException("Board not found"));
        requireRoomMembership(userId, board.getRoomId());
        UUID deletedTaskId = task.getId();
        taskRepo.delete(task);
        eventPublisher.publishEvent(new TaskDeletedEvent(deletedTaskId));
    }

    // ---- Checklist ----

    @Transactional
    public ChecklistItemResponse addChecklistItem(UUID taskId, UUID userId, String title) {
        var task = requireTask(taskId);
        var board = boardRepo.findById(task.getBoardId())
                .orElseThrow(() -> new ResourceNotFoundException("Board not found"));
        requireRoomMembership(userId, board.getRoomId());

        int maxPosition = checklistRepo.findByTaskIdOrderByPosition(taskId)
                .stream().mapToInt(ChecklistItem::getPosition).max().orElse(-1);

        var item = new ChecklistItem();
        item.setTaskId(taskId);
        item.setTitle(title);
        item.setChecked(false);
        item.setPosition(maxPosition + 1);
        checklistRepo.save(item);

        return new ChecklistItemResponse(item.getId(), item.getTitle(), item.isChecked(), item.getPosition());
    }

    @Transactional
    public ChecklistItemResponse toggleChecklistItem(UUID itemId, UUID userId) {
        var item = checklistRepo.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist item not found: " + itemId));
        var task = requireTask(item.getTaskId());
        var board = boardRepo.findById(task.getBoardId())
                .orElseThrow(() -> new ResourceNotFoundException("Board not found"));
        requireRoomMembership(userId, board.getRoomId());

        item.setChecked(!item.isChecked());
        checklistRepo.save(item);

        return new ChecklistItemResponse(item.getId(), item.getTitle(), item.isChecked(), item.getPosition());
    }

    @Transactional
    public void deleteChecklistItem(UUID itemId, UUID userId) {
        var item = checklistRepo.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist item not found: " + itemId));
        var task = requireTask(item.getTaskId());
        var board = boardRepo.findById(task.getBoardId())
                .orElseThrow(() -> new ResourceNotFoundException("Board not found"));
        requireRoomMembership(userId, board.getRoomId());

        checklistRepo.delete(item);
    }

    // ---- Columns ----

    @Transactional
    public TaskColumnResponse addColumn(UUID roomId, UUID userId, CreateColumnRequest request) {
        requireRoomMembership(userId, roomId);
        var board = getOrCreateBoard(roomId);

        int maxPosition = columnRepo.findByBoardIdOrderByPosition(board.getId())
                .stream().mapToInt(TaskColumn::getPosition).max().orElse(-1);

        var col = new TaskColumn();
        col.setBoardId(board.getId());
        col.setName(request.name());
        col.setPosition(maxPosition + 1);
        columnRepo.save(col);

        return new TaskColumnResponse(col.getId(), col.getName(), col.getPosition());
    }

    @Transactional
    public TaskColumnResponse updateColumn(UUID columnId, UUID userId, UpdateColumnRequest request) {
        var column = columnRepo.findById(columnId)
                .orElseThrow(() -> new ResourceNotFoundException("Column not found"));
        var board = boardRepo.findById(column.getBoardId())
                .orElseThrow(() -> new ResourceNotFoundException("Board not found"));
        requireRoomMembership(userId, board.getRoomId());

        if (request.name() != null && !request.name().isBlank()) column.setName(request.name());
        if (request.position() != null) column.setPosition(request.position());
        columnRepo.save(column);

        return new TaskColumnResponse(column.getId(), column.getName(), column.getPosition());
    }

    @Transactional
    public void deleteColumn(UUID columnId, UUID userId) {
        var column = columnRepo.findById(columnId)
                .orElseThrow(() -> new ResourceNotFoundException("Column not found"));
        var board = boardRepo.findById(column.getBoardId())
                .orElseThrow(() -> new ResourceNotFoundException("Board not found"));
        requireRoomMembership(userId, board.getRoomId());

        // Cannot delete if tasks exist in this column
        int taskCount = taskRepo.countByColumnId(columnId);
        if (taskCount > 0) {
            throw new BadRequestException("Cannot delete column with existing tasks. Move or delete tasks first.");
        }

        // Cannot delete the last column
        int columnCount = columnRepo.countByBoardId(board.getId());
        if (columnCount <= 1) {
            throw new BadRequestException("Cannot delete the last column.");
        }

        columnRepo.delete(column);
    }

    // ---- Re-indexing ----

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> findAllTasksForIndexing() {
        return taskRepo.findAll().stream()
                .map(t -> {
                    Map<String, Object> data = new LinkedHashMap<>();
                    data.put("id", t.getId());
                    // Resolve room from board
                    var board = boardRepo.findById(t.getBoardId()).orElse(null);
                    data.put("roomId", board != null ? board.getRoomId() : null);
                    data.put("title", t.getTitle());
                    data.put("description", t.getDescription());
                    return data;
                })
                .toList();
    }

    // ---- DSGVO ----

    @Override
    @Transactional
    public Map<String, Object> exportUserData(UUID userId) {
        Map<String, Object> data = new LinkedHashMap<>();
        var createdTasks = taskRepo.findByCreatedBy(userId);
        data.put("createdTasks", createdTasks.stream().map(t -> Map.of(
                "id", t.getId(),
                "title", t.getTitle(),
                "createdAt", t.getCreatedAt()
        )).toList());
        var assignedTasks = taskRepo.findByAssigneeId(userId);
        data.put("assignedTasks", assignedTasks.stream().map(t -> Map.of(
                "id", t.getId(),
                "title", t.getTitle()
        )).toList());
        return data;
    }

    // ---- Helpers ----

    private Task requireTask(UUID taskId) {
        return taskRepo.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));
    }

    private void requireRoomMembership(UUID userId, UUID roomId) {
        if (!roomModule.isUserInRoom(userId, roomId)) {
            throw new ForbiddenException("Not a member of this room");
        }
    }

    private TaskResponse toResponse(Task task) {
        String assigneeName = task.getAssigneeId() != null
                ? userModule.findById(task.getAssigneeId()).map(UserInfo::displayName).orElse(null)
                : null;
        String createdByName = userModule.findById(task.getCreatedBy())
                .map(UserInfo::displayName).orElse("Unbekannt");

        List<ChecklistItem> items = checklistRepo.findByTaskIdOrderByPosition(task.getId());
        List<ChecklistItemResponse> itemResponses = items.stream()
                .map(i -> new ChecklistItemResponse(i.getId(), i.getTitle(), i.isChecked(), i.getPosition()))
                .toList();
        int total = items.size();
        int checked = (int) items.stream().filter(ChecklistItem::isChecked).count();

        return new TaskResponse(
                task.getId(),
                task.getColumnId(),
                task.getTitle(),
                task.getDescription(),
                task.getAssigneeId(),
                assigneeName,
                task.getCreatedBy(),
                createdByName,
                task.getDueDate(),
                task.getPosition(),
                task.getCreatedAt(),
                itemResponses,
                total,
                checked
        );
    }

    private void publishTaskSaved(Task task, UUID roomId) {
        String assigneeName = task.getAssigneeId() != null
                ? userModule.findById(task.getAssigneeId()).map(UserInfo::displayName).orElse(null)
                : null;
        eventPublisher.publishEvent(new TaskSavedEvent(
                task.getId(), roomId, task.getTitle(), task.getDescription(), assigneeName));
    }

    private Map<UUID, String> resolveUserNames(Set<UUID> userIds) {
        if (userIds.isEmpty()) return Map.of();
        var users = userModule.findByIds(new ArrayList<>(userIds));
        Map<UUID, String> names = new HashMap<>();
        for (var user : users) {
            names.put(user.id(), user.displayName());
        }
        return names;
    }
}
