package com.monteweb.tasks.internal.dto;

import java.util.List;
import java.util.UUID;

public record TaskBoardResponse(
        UUID id,
        UUID roomId,
        List<TaskColumnResponse> columns,
        List<TaskResponse> tasks
) {
}
