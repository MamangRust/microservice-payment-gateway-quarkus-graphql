package com.sanedge.gateway.resource;

import com.sanedge.gateway.dto.UserDto.*;
import com.sanedge.gateway.service.UserService;
import io.smallrye.mutiny.Uni;
import com.sanedge.gateway.exception.GraphQLErrorHandler;
import org.eclipse.microprofile.graphql.*;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@GraphQLApi
@Singleton
@GraphQLErrorHandler
public class UserResource {

    @Inject
    UserService userService;

    @Query("listUsers")
    @Description("List all users")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<FindAllUserResponse> listUsers(
            @Name("page") @DefaultValue("1") int page,
            @Name("size") @DefaultValue("20") int size,
            @Name("search") String search) {
        return userService.listUsers(page, size, search);
    }

    @Query("getUser")
    @Description("Get user by ID")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<FindByIdUserResponse> getUser(@Name("id") int id) {
        return userService.getUser(id);
    }

    @Mutation("createUser")
    @Description("Create a new user")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<CreateUserResponse> createUser(@Name("body") CreateUserRequest body) {
        return userService.createUser(body);
    }

    @Mutation("updateUser")
    @Description("Update user")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<UpdateUserResponse> updateUser(@Name("id") int id, @Name("body") UpdateUserRequest body) {
        return userService.updateUser(id, body);
    }

    @Mutation("deleteUser")
    @Description("Soft-delete a user")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<TrashedUserResponse> deleteUser(@Name("id") int id) {
        return userService.deleteUser(id);
    }
}