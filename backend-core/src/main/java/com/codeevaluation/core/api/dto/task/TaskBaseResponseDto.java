package com.codeevaluation.core.api.dto.task;

import com.codeevaluation.core.model.Task;
import lombok.Builder;

@Builder
public record TaskBaseResponseDto(
        Long id,
        String title
) {
    public static TaskBaseResponseDto from(Task task) {
        return TaskBaseResponseDto.builder()
                .id(task.getId())
                .title(task.getTitle())
                .build();
    }
}
