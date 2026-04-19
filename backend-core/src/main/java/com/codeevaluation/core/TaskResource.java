package com.codeevaluation.core;

import com.codeevaluation.core.api.dto.task.TaskCreateDto;
import com.codeevaluation.core.api.dto.task.TaskResponseDto;
import com.codeevaluation.core.service.TaskService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.jwt.JsonWebToken;

@Path("/api/tasks")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class TaskResource {

    private final JsonWebToken jsonWebToken;
    private final TaskService taskService;

    @POST
    @RolesAllowed({ "ADMIN", "PROF" })
    public Response createTask(TaskCreateDto taskCreateDto) {
        String username = jsonWebToken.getSubject();
        TaskResponseDto taskResponseDto = taskService.createTask(taskCreateDto, username);
        return Response.created(URI.create("/tasks/" + taskResponseDto.getId()))
                .entity(taskResponseDto)
                .build();
    }
}
