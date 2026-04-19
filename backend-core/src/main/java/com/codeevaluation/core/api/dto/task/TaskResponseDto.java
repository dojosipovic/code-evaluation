package com.codeevaluation.core.api.dto.task;

import com.codeevaluation.core.api.dto.user.UserDto;
import com.codeevaluation.core.enumeration.TaskStatus;
import com.codeevaluation.core.model.Task;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
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
        TaskResponseDto taskResponseDto = new TaskResponseDto();

        taskResponseDto.setId(task.getId());
        taskResponseDto.setTitle(task.getTitle());
        taskResponseDto.setDescription(task.getDescription());
        taskResponseDto.setStarterCode(new StarterCodeDto(null, task.getStarterCode()));
        taskResponseDto.setShared(task.getShared());
        taskResponseDto.setEnabled(task.getEnabled());
        taskResponseDto.setStatus(task.getStatus());
        taskResponseDto.setTests(TestResponseDto.from(task.getTests()));
        taskResponseDto.setUser(UserDto.from(task.getUser()));

        return taskResponseDto;
    }
}
