package com.codeevaluation.core;

import com.codeevaluation.core.api.dto.group.GroupCreateDto;
import com.codeevaluation.core.api.dto.group.GroupResponseDto;
import com.codeevaluation.core.service.GroupService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import lombok.RequiredArgsConstructor;

@Path("/api/groups")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class GroupResource {

    private final GroupService groupService;

    @POST
    @RolesAllowed({ "ADMIN", "PROF" })
    public Response createGroup(GroupCreateDto groupCreateDto) {
        GroupResponseDto groupResponseDto = groupService.createGroup(groupCreateDto);
        return Response.created(URI.create("/groups/" + groupResponseDto.getId()))
                .entity(groupResponseDto)
                .build();
    }
}
