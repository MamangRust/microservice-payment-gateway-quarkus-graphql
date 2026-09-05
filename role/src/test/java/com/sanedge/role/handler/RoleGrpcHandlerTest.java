package com.sanedge.role.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.common.domain.response.PaginationMeta;
import com.sanedge.role.domain.requests.FindAllRoles;
import com.sanedge.role.domain.response.RoleResponse;
import com.sanedge.role.domain.response.RoleResponseDeleteAt;
import com.sanedge.role.service.RoleCommandService;
import com.sanedge.role.service.RoleQueryService;

import io.smallrye.mutiny.Uni;
import pb.role.Role.ApiResponseRole;
import pb.role.Role.ApiResponseRoleDeleteAt;
import pb.role.Role.FindAllRoleRequest;
import pb.role.Role.FindByIdRoleRequest;
import pb.role.RoleCommand.ApiResponseRoleAll;
import pb.role.RoleCommand.ApiResponseRoleDelete;
import pb.role.RoleCommand.CreateRoleRequest;
import pb.role.RoleCommand.UpdateRoleRequest;
import pb.role.RoleQuery.ApiResponsePaginationRole;
import pb.role.RoleQuery.ApiResponsePaginationRoleDeleteAt;
import pb.role.RoleQuery.FindByNameRoleRequest;

@ExtendWith(MockitoExtension.class)
class RoleGrpcHandlerTest {
        @Mock
        private RoleCommandService roleCommandService;

        @Mock
        private RoleQueryService roleQueryService;

        @Nested
        class RoleCommandGrpcHandlerTest {

                private RoleCommandGrpcHandler commandHandler;

                @BeforeEach
                void setUp() {
                        commandHandler = new RoleCommandGrpcHandler();
                        injectDependencies(commandHandler);
                }

                @Test
                void createRole_success_mapsToProtoCorrectly() {
                        CreateRoleRequest request = CreateRoleRequest.newBuilder()
                                        .setName("Admin").build();

                        RoleResponse domainRes = createDomainRoleResponse();
                        when(roleCommandService.create(any()))
                                        .thenReturn(Uni.createFrom().item(ApiResponse.success("Success", domainRes)));

                        ApiResponseRole response = commandHandler.createRole(request).await().indefinitely();

                        assertThat(response.getStatus()).isEqualTo("success");
                        assertThat(response.getData().getName()).isEqualTo("Admin");
                        assertThat(response.getData().getId()).isEqualTo(1);
                }

                @Test
                void createRole_withNullDates_handlesGracefully() {
                        CreateRoleRequest request = CreateRoleRequest.newBuilder().setName("Guest").build();
                        RoleResponse domainRes = RoleResponse.builder().id(1).name("Guest").createdAt(null)
                                        .updatedAt(null)
                                        .build();

                        when(roleCommandService.create(any()))
                                        .thenReturn(Uni.createFrom().item(ApiResponse.success("Success", domainRes)));

                        ApiResponseRole response = commandHandler.createRole(request).await().indefinitely();

                        assertThat(response.getData().getCreatedAt()).isEmpty();
                        assertThat(response.getData().getUpdatedAt()).isEmpty();
                }

                @Test
                void updateRole_success_mapsToProtoCorrectly() {
                        UpdateRoleRequest request = UpdateRoleRequest.newBuilder().setId(1).setName("Editor").build();
                        RoleResponse domainRes = createDomainRoleResponse();

                        when(roleCommandService.update(any()))
                                        .thenReturn(Uni.createFrom().item(ApiResponse.success("Updated", domainRes)));

                        ApiResponseRole response = commandHandler.updateRole(request).await().indefinitely();

                        assertThat(response.getStatus()).isEqualTo("success");
                        assertThat(response.getData().getName()).isEqualTo("Admin");
                }

                @Test
                void trashedRole_success_mapsDeletedAtToStringValue() {
                        FindByIdRoleRequest request = FindByIdRoleRequest.newBuilder().setRoleId(1).build();
                        RoleResponseDeleteAt domainRes = createDomainRoleResponseDeleteAt();

                        when(roleCommandService.trash(any()))
                                        .thenReturn(Uni.createFrom().item(ApiResponse.success("Trashed", domainRes)));

                        ApiResponseRoleDeleteAt response = commandHandler.trashedRole(request).await().indefinitely();

                        assertThat(response.getStatus()).isEqualTo("success");
                        assertThat(response.getData().hasDeletedAt()).isTrue();
                        assertThat(response.getData().getDeletedAt().getValue()).contains("2023-10-10");
                }

                @Test
                void trashedRole_withNullDeletedAt_doesNotSetStringValue() {
                        FindByIdRoleRequest request = FindByIdRoleRequest.newBuilder().setRoleId(1).build();
                        RoleResponseDeleteAt domainRes = RoleResponseDeleteAt.builder().id(1).name("Admin")
                                        .deletedAt(null)
                                        .build();

                        when(roleCommandService.trash(any()))
                                        .thenReturn(Uni.createFrom().item(ApiResponse.success("Trashed", domainRes)));

                        ApiResponseRoleDeleteAt response = commandHandler.trashedRole(request).await().indefinitely();

                        assertThat(response.getData().hasDeletedAt()).isFalse();
                }

                @Test
                void restoreRole_success_mapsToProtoCorrectly() {
                        FindByIdRoleRequest request = FindByIdRoleRequest.newBuilder().setRoleId(1).build();
                        RoleResponseDeleteAt domainRes = createDomainRoleResponseDeleteAt();

                        when(roleCommandService.restore(any()))
                                        .thenReturn(Uni.createFrom().item(ApiResponse.success("Restored", domainRes)));

                        ApiResponseRoleDeleteAt response = commandHandler.restoreRole(request).await().indefinitely();

                        assertThat(response.getStatus()).isEqualTo("success");
                        assertThat(response.getData().getName()).isEqualTo("Admin");
                }

                @Test
                void deleteRolePermanent_success_returnsVoidProto() {
                        FindByIdRoleRequest request = FindByIdRoleRequest.newBuilder().setRoleId(1).build();

                        when(roleCommandService.deletePermanent(any()))
                                        .thenReturn(Uni.createFrom().item(ApiResponse.success("Deleted")));

                        ApiResponseRoleDelete response = commandHandler.deleteRolePermanent(request).await()
                                        .indefinitely();

                        assertThat(response.getStatus()).isEqualTo("success");
                        assertThat(response.getMessage()).isEqualTo("Deleted");
                }

                @Test
                void restoreAllRole_success_returnsVoidProto() {
                        when(roleCommandService.restoreAllTrashedRoles())
                                        .thenReturn(Uni.createFrom().item(ApiResponse.success("All Restored")));

                        ApiResponseRoleAll response = commandHandler
                                        .restoreAllRole(com.google.protobuf.Empty.getDefaultInstance())
                                        .await().indefinitely();

                        assertThat(response.getStatus()).isEqualTo("success");
                }

                @Test
                void deleteAllRolePermanent_success_returnsVoidProto() {
                        when(roleCommandService.deleteAllTrashedRoles())
                                        .thenReturn(Uni.createFrom().item(ApiResponse.success("All Deleted")));

                        ApiResponseRoleAll response = commandHandler
                                        .deleteAllRolePermanent(com.google.protobuf.Empty.getDefaultInstance()).await()
                                        .indefinitely();

                        assertThat(response.getStatus()).isEqualTo("success");
                }
        }

        @Nested
        class RoleQueryGrpcHandlerTest {

                private RoleQueryGrpcHandler queryHandler;

                @BeforeEach
                void setUp() {
                        queryHandler = new RoleQueryGrpcHandler();
                        try {
                                java.lang.reflect.Field queryField = RoleQueryGrpcHandler.class
                                                .getDeclaredField("roleQueryService");
                                queryField.setAccessible(true);
                                queryField.set(queryHandler, roleQueryService);
                        } catch (Exception e) {
                                throw new RuntimeException(e);
                        }
                }

                @Test
                void findByIdRole_success_mapsToProtoCorrectly() {
                        FindByIdRoleRequest request = FindByIdRoleRequest.newBuilder().setRoleId(1).build();
                        RoleResponse domainRes = createDomainRoleResponse();

                        when(roleQueryService.findById(any()))
                                        .thenReturn(Uni.createFrom().item(ApiResponse.success("Found", domainRes)));

                        ApiResponseRole response = queryHandler.findByIdRole(request).await().indefinitely();

                        assertThat(response.getStatus()).isEqualTo("success");
                        assertThat(response.getData().getId()).isEqualTo(1);
                        assertThat(response.getData().getName()).isEqualTo("Admin");
                }

                @Test
                void findByIdRole_whenDataIsNull_doesNotSetData() {
                        FindByIdRoleRequest request = FindByIdRoleRequest.newBuilder().setRoleId(99).build();

                        when(roleQueryService.findById(any()))
                                        .thenReturn(Uni.createFrom().item(ApiResponse.success("Not found", null)));

                        ApiResponseRole response = queryHandler.findByIdRole(request).await().indefinitely();

                        assertThat(response.hasData()).isFalse();
                }

                @Test
                void findByNameRole_success_mapsToProtoCorrectly() {
                        FindByNameRoleRequest request = FindByNameRoleRequest.newBuilder().setName("Admin").build();
                        RoleResponse domainRes = createDomainRoleResponse();

                        when(roleQueryService.findByName(any()))
                                        .thenReturn(Uni.createFrom().item(ApiResponse.success("Found", domainRes)));

                        ApiResponseRole response = queryHandler.findByNameRole(request).await().indefinitely();

                        assertThat(response.getStatus()).isEqualTo("success");
                        assertThat(response.getData().getName()).isEqualTo("Admin");
                        assertThat(response.getData().getId()).isEqualTo(1);
                }

                @Test
                void findByNameRole_whenDataIsNull_doesNotSetData() {
                        FindByNameRoleRequest request = FindByNameRoleRequest.newBuilder().setName("Unknown").build();

                        when(roleQueryService.findByName(any()))
                                        .thenReturn(Uni.createFrom().item(ApiResponse.success("Not found", null)));

                        ApiResponseRole response = queryHandler.findByNameRole(request).await().indefinitely();

                        assertThat(response.hasData()).isFalse();
                }

                @Test
                void findAllRole_success_mapsListAndPaginationCorrectly() {
                        FindAllRoleRequest request = FindAllRoleRequest.newBuilder().setPage(1).setPageSize(10)
                                        .setSearch("")
                                        .build();

                        List<RoleResponse> mockList = List.of(createDomainRoleResponse(), createDomainRoleResponse());
                        PaginationMeta meta = new PaginationMeta(1, 10, 1, 2);
                        ApiResponsePagination<List<RoleResponse>> serviceRes = new ApiResponsePagination<>("success",
                                        "Found",
                                        mockList, meta);

                        when(roleQueryService.findAllPaginated(any(FindAllRoles.class)))
                                        .thenReturn(Uni.createFrom().item(serviceRes));

                        ApiResponsePaginationRole response = queryHandler.findAllRole(request).await().indefinitely();

                        assertThat(response.getStatus()).isEqualTo("success");
                        assertThat(response.getDataCount()).isEqualTo(2);
                        assertThat(response.getPaginationMeta().getTotalRecords()).isEqualTo(2);
                        assertThat(response.getPaginationMeta().getTotalPages()).isEqualTo(1);
                        assertThat(response.getPaginationMeta().getCurrentPage()).isEqualTo(1);
                }

                @Test
                void findAllRole_withNullData_doesNotAddData() {
                        FindAllRoleRequest request = FindAllRoleRequest.newBuilder().setPage(1).setPageSize(10).build();
                        ApiResponsePagination<List<RoleResponse>> serviceRes = new ApiResponsePagination<>("success",
                                        "Found",
                                        null, null);

                        when(roleQueryService.findAllPaginated(any(FindAllRoles.class)))
                                        .thenReturn(Uni.createFrom().item(serviceRes));

                        ApiResponsePaginationRole response = queryHandler.findAllRole(request).await().indefinitely();

                        assertThat(response.getDataCount()).isEqualTo(0);
                        assertThat(response.hasPaginationMeta()).isFalse();
                }

                @Test
                void findAllRole_withNullPagination_doesNotSetPagination() {
                        FindAllRoleRequest request = FindAllRoleRequest.newBuilder().setPage(1).setPageSize(10).build();
                        ApiResponsePagination<List<RoleResponse>> serviceRes = new ApiResponsePagination<>("success",
                                        "Found",
                                        List.of(), null);

                        when(roleQueryService.findAllPaginated(any(FindAllRoles.class)))
                                        .thenReturn(Uni.createFrom().item(serviceRes));

                        ApiResponsePaginationRole response = queryHandler.findAllRole(request).await().indefinitely();

                        assertThat(response.hasPaginationMeta()).isFalse();
                }

                @Test
                void findByActive_success_mapsDeletedAtToStringValue() {
                        FindAllRoleRequest request = FindAllRoleRequest.newBuilder().setPage(1).setPageSize(10).build();

                        List<RoleResponseDeleteAt> mockList = List.of(createDomainRoleResponseDeleteAt());
                        PaginationMeta meta = new PaginationMeta(1, 10, 1, 1);
                        ApiResponsePagination<List<RoleResponseDeleteAt>> serviceRes = new ApiResponsePagination<>(
                                        "success",
                                        "Found", mockList, meta);

                        when(roleQueryService.findActivePaginated(any(FindAllRoles.class)))
                                        .thenReturn(Uni.createFrom().item(serviceRes));

                        ApiResponsePaginationRoleDeleteAt response = queryHandler.findByActive(request).await()
                                        .indefinitely();

                        assertThat(response.getDataCount()).isEqualTo(1);
                        assertThat(response.getData(0).hasDeletedAt()).isTrue();
                        assertThat(response.getData(0).getDeletedAt().getValue()).contains("2023-10-10");
                }

                @Test
                void findByActive_withNullDeletedAt_doesNotSetStringValue() {
                        FindAllRoleRequest request = FindAllRoleRequest.newBuilder().setPage(1).setPageSize(10).build();

                        RoleResponseDeleteAt nullDeletedAt = RoleResponseDeleteAt.builder()
                                        .id(2).name("Viewer")
                                        .createdAt(LocalDateTime.now().toString())
                                        .updatedAt(LocalDateTime.now().toString())
                                        .deletedAt(null)
                                        .build();
                        List<RoleResponseDeleteAt> mockList = List.of(nullDeletedAt);
                        PaginationMeta meta = new PaginationMeta(1, 10, 1, 1);
                        ApiResponsePagination<List<RoleResponseDeleteAt>> serviceRes = new ApiResponsePagination<>(
                                        "success",
                                        "Found", mockList, meta);

                        when(roleQueryService.findActivePaginated(any(FindAllRoles.class)))
                                        .thenReturn(Uni.createFrom().item(serviceRes));

                        ApiResponsePaginationRoleDeleteAt response = queryHandler.findByActive(request).await()
                                        .indefinitely();

                        assertThat(response.getData(0).hasDeletedAt()).isFalse();
                }

                @Test
                void findByTrashed_success_mapsCorrectly() {
                        FindAllRoleRequest request = FindAllRoleRequest.newBuilder().setPage(1).setPageSize(10).build();

                        List<RoleResponseDeleteAt> mockList = List.of(createDomainRoleResponseDeleteAt());
                        PaginationMeta meta = new PaginationMeta(1, 10, 1, 1);
                        ApiResponsePagination<List<RoleResponseDeleteAt>> serviceRes = new ApiResponsePagination<>(
                                        "success",
                                        "Found", mockList, meta);

                        when(roleQueryService.findTrashedPaginated(any(FindAllRoles.class)))
                                        .thenReturn(Uni.createFrom().item(serviceRes));

                        ApiResponsePaginationRoleDeleteAt response = queryHandler.findByTrashed(request).await()
                                        .indefinitely();

                        assertThat(response.getStatus()).isEqualTo("success");
                        assertThat(response.getPaginationMeta().getTotalRecords()).isEqualTo(1);
                        assertThat(response.getData(0).getName()).isEqualTo("Admin");
                }

                @Test
                void findByTrashed_withNullData_doesNotAddData() {
                        FindAllRoleRequest request = FindAllRoleRequest.newBuilder().setPage(1).setPageSize(10).build();
                        ApiResponsePagination<List<RoleResponseDeleteAt>> serviceRes = new ApiResponsePagination<>(
                                        "success",
                                        "Found", null, null);

                        when(roleQueryService.findTrashedPaginated(any(FindAllRoles.class)))
                                        .thenReturn(Uni.createFrom().item(serviceRes));

                        ApiResponsePaginationRoleDeleteAt response = queryHandler.findByTrashed(request).await()
                                        .indefinitely();

                        assertThat(response.getDataCount()).isEqualTo(0);
                        assertThat(response.hasPaginationMeta()).isFalse();
                }

                @Test
                void findByTrashed_withNullPagination_doesNotSetPagination() {
                        FindAllRoleRequest request = FindAllRoleRequest.newBuilder().setPage(1).setPageSize(10).build();
                        ApiResponsePagination<List<RoleResponseDeleteAt>> serviceRes = new ApiResponsePagination<>(
                                        "success",
                                        "Found", List.of(), null);

                        when(roleQueryService.findTrashedPaginated(any(FindAllRoles.class)))
                                        .thenReturn(Uni.createFrom().item(serviceRes));

                        ApiResponsePaginationRoleDeleteAt response = queryHandler.findByTrashed(request).await()
                                        .indefinitely();

                        assertThat(response.hasPaginationMeta()).isFalse();
                }
        }

        private void injectDependencies(Object handler) {
                try {
                        java.lang.reflect.Field commandField = handler.getClass()
                                        .getDeclaredField("roleCommandService");
                        commandField.setAccessible(true);
                        commandField.set(handler, roleCommandService);
                } catch (NoSuchFieldException e) {
                        // ignore
                } catch (Exception e) {
                        throw new RuntimeException("Failed to inject roleCommandService via reflection", e);
                }

                try {
                        java.lang.reflect.Field queryField = handler.getClass().getDeclaredField("roleQueryService");
                        queryField.setAccessible(true);
                        queryField.set(handler, roleQueryService);
                } catch (NoSuchFieldException e) {
                        // ignore
                } catch (Exception e) {
                        throw new RuntimeException("Failed to inject roleQueryService via reflection", e);
                }
        }

        private RoleResponse createDomainRoleResponse() {
                return RoleResponse.builder()
                                .id(1)
                                .name("Admin")
                                .createdAt(LocalDateTime.now().toString())
                                .updatedAt(LocalDateTime.now().toString())
                                .build();
        }

        private RoleResponseDeleteAt createDomainRoleResponseDeleteAt() {
                return RoleResponseDeleteAt.builder()
                                .id(1)
                                .name("Admin")
                                .createdAt(LocalDateTime.now().toString())
                                .updatedAt(LocalDateTime.now().toString())
                                .deletedAt(LocalDateTime.of(2023, 10, 10, 10, 10).toString())
                                .build();
        }
}