package com.codeevaluation.core;

import com.codeevaluation.core.api.dto.PagedResponse;
import com.codeevaluation.core.api.dto.task.TaskCreateDto;
import com.codeevaluation.core.api.dto.task.TaskListItemDto;
import com.codeevaluation.core.api.dto.task.TaskPatchDto;
import com.codeevaluation.core.api.dto.task.TaskResponseDto;
import com.codeevaluation.core.api.dto.task.TaskUpdateDto;
import com.codeevaluation.core.service.TaskService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import lombok.RequiredArgsConstructor;

@Path("/api/tasks")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class TaskResource {

    private final TaskService taskService;

    @POST
    @RolesAllowed({ "ADMIN", "PROF" })
    public Response createTask(TaskCreateDto taskCreateDto) {
        TaskResponseDto taskResponseDto = taskService.createTask(taskCreateDto);
        return Response.created(URI.create("/tasks/" + taskResponseDto.getId()))
                .entity(taskResponseDto)
                .build();
    }

    @PUT
    @RolesAllowed({ "ADMIN", "PROF" })
    @Path("/{id}")
    public TaskResponseDto updateTask(TaskUpdateDto taskUpdateDto, @PathParam("id") Long id) {
        return taskService.updateTask(taskUpdateDto, id);
    }

    @POST
    @RolesAllowed({ "ADMIN", "PROF" })
    @Path("/{id}/publish")
    public TaskResponseDto publishTask(@PathParam("id") Long id) {
        return taskService.publishTask(id);
    }

    @PATCH
    @RolesAllowed({ "ADMIN", "PROF" })
    @Path("/{id}")
    public TaskResponseDto patchTask(TaskPatchDto taskPatchDto, @PathParam("id") Long id) {
        return taskService.patchTask(taskPatchDto, id);
    }

    @GET
    @RolesAllowed({ "ADMIN", "PROF" })
    @Path("/{id}")
    public TaskResponseDto getTask(@PathParam("id") Long id) {
        return taskService.getTask(id);
    }

    @DELETE
    @RolesAllowed({ "ADMIN", "PROF" })
    @Path("/{id}")
    public Response deleteTask(@PathParam("id") Long id) {
        taskService.deleteTask(id);
        return Response.noContent().build();
    }

    @GET
    @Path("/me")
    @RolesAllowed({ "ADMIN", "PROF" })
    public PagedResponse<TaskListItemDto> getMyTasks(
            @BeanParam TaskListQueryParams taskListQueryParams) {
        return taskService.getMyTasks(taskListQueryParams);
    }

    @GET
    @Path("/others")
    @RolesAllowed({ "ADMIN", "PROF" })
    public PagedResponse<TaskListItemDto> getOtherTasks(
            @BeanParam TaskListQueryParams taskListQueryParams) {
        return taskService.getOtherTasks(taskListQueryParams);
    }
}
