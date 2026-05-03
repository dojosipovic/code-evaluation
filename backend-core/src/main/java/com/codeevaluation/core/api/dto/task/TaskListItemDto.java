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
public class TaskListItemDto {
    private Long id;
    private String title;
    private TaskStatus status;
    private Boolean enabled;
    private Boolean shared;
    private UserDto user;

    public static TaskListItemDto from(Task task) {
        TaskListItemDto taskListItemDto = new TaskListItemDto();

        taskListItemDto.setId(task.getId());
        taskListItemDto.setTitle(task.getTitle());
        taskListItemDto.setStatus(task.getStatus());
        taskListItemDto.setEnabled(task.getEnabled());
        taskListItemDto.setShared(task.getShared());
        taskListItemDto.setUser(UserDto.from(task.getUser()));

        return taskListItemDto;
    }

    public static List<TaskListItemDto> from(List<Task> tasks) {
        return tasks.stream().map(TaskListItemDto::from).toList();
    }
}
