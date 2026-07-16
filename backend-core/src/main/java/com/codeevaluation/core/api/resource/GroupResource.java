package com.codeevaluation.core.api.resource;

import com.codeevaluation.core.api.dto.PagedResponse;
import com.codeevaluation.core.api.dto.assignment.AssignmentCreateDto;
import com.codeevaluation.core.api.dto.assignment.AssignmentListItemDto;
import com.codeevaluation.core.api.dto.assignment.AssignmentResponseDto;
import com.codeevaluation.core.api.dto.group.GroupCreateDto;
import com.codeevaluation.core.api.dto.group.GroupListItemDto;
import com.codeevaluation.core.api.dto.group.GroupMemberDto;
import com.codeevaluation.core.api.dto.group.GroupResponseDto;
import com.codeevaluation.core.api.dto.group.GroupUpdateDto;
import com.codeevaluation.core.api.dto.user.UserDto;
import com.codeevaluation.core.api.query.PagedParams;
import com.codeevaluation.core.service.AssignmentService;
import com.codeevaluation.core.service.GroupService;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
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

    @GET
    @Path("/{groupId}")
    @Authenticated
    public GroupResponseDto getGroup(@PathParam("groupId") Long groupId) {
        return groupService.findById(groupId);
    }

    @PUT
    @Path("/{groupId}")
    @RolesAllowed({ "ADMIN", "PROF" })
    public GroupResponseDto updateGroup(
            @PathParam("groupId") Long groupId,
            GroupUpdateDto groupCreateDto
    ) {
        return groupService.updateGroup(groupCreateDto, groupId);
    }

    @GET
    @Authenticated
    public PagedResponse<GroupListItemDto> getGroups(@BeanParam PagedParams pagedParams) {
        return groupService.getGroups(pagedParams);
    }

    @POST
    @Path("/{groupId}/members/{userId}")
    @RolesAllowed({ "ADMIN", "PROF" })
    public Response addMember(
            @PathParam("groupId") Long groupId,
            @PathParam("userId") Long userId
    ) {
        groupService.addMember(groupId, userId);
        return Response.status(Response.Status.CREATED).build();
    }

    @DELETE
    @Path("/{groupId}/members/{userId}")
    @RolesAllowed({ "ADMIN", "PROF" })
    public Response removeMember(
            @PathParam("groupId") Long groupId,
            @PathParam("userId") Long userId
    ) {
        groupService.removeMember(groupId, userId);
        return Response.noContent().build();
    }

    @GET
    @Path("/{groupId}/members")
    @Authenticated
    public PagedResponse<GroupMemberDto> getMembers(
            @PathParam("groupId") Long groupId,
            @BeanParam PagedParams pagedParams
    ) {
        return groupService.getMembers(groupId, pagedParams);
    }

    @GET
    @Path("/{groupId}/non-members")
    @RolesAllowed({ "ADMIN", "PROF" })
    public PagedResponse<UserDto> getNonMembers(
            @PathParam("groupId") Long groupId,
            @BeanParam PagedParams pagedParams
    ) {
        return groupService.getNonMembers(groupId, pagedParams);
    }
}
