package com.codeevaluation.core.api.dto.task;

import com.codeevaluation.core.api.dto.user.UserDto;
import com.codeevaluation.core.enumeration.TaskStatus;
import com.codeevaluation.core.model.Task;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TaskResponseDto {

    private Long id;
    private String title;
    private String description;
    private StarterCodeDto starterCode;
    private Boolean includeStarterCode;
    private Boolean shared;
    private Boolean enabled;
    private TaskStatus status;
    private List<TestResponseDto> tests;
    private UserDto user;

    public static TaskResponseDto from(Task task) {
        return buildGeneralInfo(task)
                .tests(TestResponseDto.from(task.getTests()))
                .build();
    }

    public static TaskResponseDto from(Task task, boolean showTestExpectedOutput) {
        return buildGeneralInfo(task)
                .tests(TestResponseDto.from(task.getTests(), showTestExpectedOutput))
                .build();
    }

    private static TaskResponseDtoBuilder buildGeneralInfo(Task task) {
        return TaskResponseDto.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .starterCode(new StarterCodeDto(null, task.getStarterCode()))
                .includeStarterCode(task.getIncludeStarterCode())
                .shared(task.getShared())
                .enabled(task.getEnabled())
                .status(task.getStatus())
                .user(UserDto.from(task.getUser()));
    }
}
