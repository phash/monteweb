package com.monteweb.tasks.internal.controller;

import com.monteweb.shared.dto.ApiResponse;
import com.monteweb.shared.util.SecurityUtils;
import com.monteweb.tasks.internal.dto.*;
import com.monteweb.tasks.internal.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rooms/{roomId}/tasks")
@ConditionalOnProperty(prefix = "monteweb.modules", name = "tasks.enabled", havingValue = "true")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    public ApiResponse<TaskBoardResponse> getBoard(@PathVariable UUID roomId) {
        SecurityUtils.requireCurrentUserId();
        return ApiResponse.ok(taskService.getBoard(roomId));
    }

    @PostMapping
    public ApiResponse<TaskResponse> createTask(
            @PathVariable UUID roomId,
            @Valid @RequestBody CreateTaskRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        return ApiResponse.ok(taskService.createTask(roomId, userId, request));
    }

    @PutMapping("/{taskId}")
    public ApiResponse<TaskResponse> updateTask(
            @PathVariable UUID roomId,
            @PathVariable UUID taskId,
            @Valid @RequestBody UpdateTaskRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        return ApiResponse.ok(taskService.updateTask(taskId, userId, request));
    }

    @PutMapping("/{taskId}/move")
    public ApiResponse<TaskResponse> moveTask(
            @PathVariable UUID roomId,
            @PathVariable UUID taskId,
            @Valid @RequestBody MoveTaskRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        return ApiResponse.ok(taskService.moveTask(taskId, userId, request.columnId(), request.position()));
    }

    @DeleteMapping("/{taskId}")
    public ApiResponse<Void> deleteTask(
            @PathVariable UUID roomId,
            @PathVariable UUID taskId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        taskService.deleteTask(taskId, userId);
        return ApiResponse.ok(null);
    }

    // ---- Checklist ----

    @PostMapping("/{taskId}/checklist")
    public ApiResponse<ChecklistItemResponse> addChecklistItem(
            @PathVariable UUID roomId,
            @PathVariable UUID taskId,
            @Valid @RequestBody CreateChecklistItemRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        return ApiResponse.ok(taskService.addChecklistItem(taskId, userId, request.title()));
    }

    @PutMapping("/{taskId}/checklist/{itemId}/toggle")
    public ApiResponse<ChecklistItemResponse> toggleChecklistItem(
            @PathVariable UUID roomId,
            @PathVariable UUID taskId,
            @PathVariable UUID itemId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        return ApiResponse.ok(taskService.toggleChecklistItem(itemId, userId));
    }

    @DeleteMapping("/{taskId}/checklist/{itemId}")
    public ApiResponse<Void> deleteChecklistItem(
            @PathVariable UUID roomId,
            @PathVariable UUID taskId,
            @PathVariable UUID itemId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        taskService.deleteChecklistItem(itemId, userId);
        return ApiResponse.ok(null);
    }

    // ---- Columns ----

    @PostMapping("/columns")
    public ApiResponse<TaskColumnResponse> addColumn(
            @PathVariable UUID roomId,
            @Valid @RequestBody CreateColumnRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        return ApiResponse.ok(taskService.addColumn(roomId, userId, request));
    }

    @PutMapping("/columns/{columnId}")
    public ApiResponse<TaskColumnResponse> updateColumn(
            @PathVariable UUID roomId,
            @PathVariable UUID columnId,
            @Valid @RequestBody UpdateColumnRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        return ApiResponse.ok(taskService.updateColumn(columnId, userId, request));
    }

    @DeleteMapping("/columns/{columnId}")
    public ApiResponse<Void> deleteColumn(
            @PathVariable UUID roomId,
            @PathVariable UUID columnId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        taskService.deleteColumn(columnId, userId);
        return ApiResponse.ok(null);
    }
}
