package com.sanedge.gateway.service;

import com.sanedge.gateway.dto.UserDto.*;
import io.smallrye.mutiny.Uni;

public interface UserService {
    Uni<FindAllUserResponse> listUsers(int page, int size, String search);
    Uni<FindByIdUserResponse> getUser(int id);
    Uni<CreateUserResponse> createUser(CreateUserRequest body);
    Uni<UpdateUserResponse> updateUser(int id, UpdateUserRequest body);
    Uni<TrashedUserResponse> deleteUser(int id);
}
