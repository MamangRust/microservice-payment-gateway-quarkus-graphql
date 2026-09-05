package com.sanedge.role.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sanedge.role.domain.requests.FindAllRoles;
import com.sanedge.role.entity.Role;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import com.sanedge.common.test.PostgreSqlResource;import io.quarkus.test.common.QuarkusTestResource;

@QuarkusTestResource(PostgreSqlResource.class)
@QuarkusTest
@RunOnVertxContext
class RoleRepositoryTest {

    @Inject
    RoleRepository roleRepository;

    private Uni<Role> createAndPersistRole(String roleName) {
        Role role = new Role();
        role.setRoleName(roleName);
        return roleRepository.persist(role).replaceWith(role);
    }

    @Test
    @WithSession
    Uni<Void> testCreateAndFindById() {
        return createAndPersistRole("Admin")
                .invoke(saved -> {
                    assertThat(saved).isNotNull();
                    assertThat(saved.id).isNotNull();
                    assertThat(saved.getRoleName()).isEqualTo("Admin");
                })
                .chain(saved -> roleRepository.findById(saved.id))
                .invoke(found -> {
                    assertThat(found).isNotNull();
                    assertThat(found.getRoleName()).isEqualTo("Admin");
                })
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testFindByRoleName() {
        return createAndPersistRole("Editor")
                .chain(ignored -> roleRepository.findByRoleName("Editor"))
                .invoke(found -> {
                    assertThat(found).isNotNull();
                    assertThat(found.getRoleName()).isEqualTo("Editor");
                })
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testFindByRoleNameReturnsNullWhenNotFound() {
        return roleRepository.findByRoleName("NonExistentRole")
                .invoke(notFound -> assertThat(notFound).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testFindByIdReturnsNullWhenNotFound() {
        return roleRepository.findById(99999L)
                .invoke(notFound -> assertThat(notFound).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testFindUserRolesReturnsEmptyWhenNoMapping() {
        return roleRepository.findUserRoles(99999L)
                .invoke(result -> assertThat(result).isEmpty())
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testTrashRole() {
        return createAndPersistRole("TrashMe")
                .invoke(saved -> assertThat(saved.getDeletedAt()).isNull())
                .chain(saved -> roleRepository.trash(saved.id))
                .invoke(trashed -> {
                    assertThat(trashed).isNotNull();
                    assertThat(trashed.getDeletedAt()).isNotNull();
                })
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testTrashRoleReturnsNullIfAlreadyTrashed() {
        return createAndPersistRole("TrashMe2")
                .chain(saved -> roleRepository.trash(saved.id)
                        .chain(ignored -> roleRepository.trash(saved.id)))
                .invoke(trashedAgain -> assertThat(trashedAgain).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testTrashRoleReturnsNullIfNotFound() {
        return roleRepository.trash(99999L)
                .invoke(trashed -> assertThat(trashed).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testRestoreRole() {
        return createAndPersistRole("RestoreMe")
                .chain(saved -> roleRepository.trash(saved.id)
                        .chain(ignored -> roleRepository.restore(saved.id)))
                .invoke(restored -> {
                    assertThat(restored).isNotNull();
                    assertThat(restored.getDeletedAt()).isNull();
                })
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testRestoreRoleReturnsNullIfNotTrashed() {
        return createAndPersistRole("RestoreMe2")
                .chain(saved -> roleRepository.restore(saved.id))
                .invoke(restored -> assertThat(restored).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testDeletePermanent() {
        return createAndPersistRole("DeleteMe")
                .chain(saved -> roleRepository.trash(saved.id)
                        .chain(ignored -> roleRepository.deletePermanent(saved.id))
                        .chain(deleted -> {
                            assertThat(deleted).isNotNull();
                            return roleRepository.findById(saved.id);
                        }))
                .invoke(checkDb -> assertThat(checkDb).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testDeletePermanentFailsIfNotTrashed() {
        return createAndPersistRole("DeleteMe2")
                .chain(saved -> roleRepository.deletePermanent(saved.id)
                        .chain(deleted -> {
                            assertThat(deleted).isNull();
                            return roleRepository.findById(saved.id);
                        }))
                .invoke(checkDb -> assertThat(checkDb).isNotNull())
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testRestoreAllDeleted() {
        return Uni.combine().all()
                .unis(createAndPersistRole("BulkRole1"),
                        createAndPersistRole("BulkRole2"),
                        createAndPersistRole("BulkRole3"))
                .asTuple()
                .chain(tuple -> Uni.join().all(
                        roleRepository.trash(tuple.getItem1().id),
                        roleRepository.trash(tuple.getItem2().id))
                        .andCollectFailures()
                        .replaceWith(tuple))
                .chain(tuple -> roleRepository.restoreAllDeleted()
                        .chain(result -> {
                            assertThat(result).isTrue();
                            return Uni.join().all(
                                    roleRepository.findById(tuple.getItem1().id),
                                    roleRepository.findById(tuple.getItem2().id),
                                    roleRepository.findById(tuple.getItem3().id))
                                    .andCollectFailures()
                                    .replaceWith(tuple);
                        }))
                .invoke(tuple -> {
                    assertThat(tuple.getItem1().getDeletedAt()).isNull();
                    assertThat(tuple.getItem2().getDeletedAt()).isNull();
                    assertThat(tuple.getItem3().getDeletedAt()).isNull();
                })
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testDeleteAllDeleted() {
        return Uni.combine().all()
                .unis(createAndPersistRole("BulkDelRole1"),
                        createAndPersistRole("BulkDelRole2"),
                        createAndPersistRole("BulkDelRole3"))
                .asTuple()
                .chain(tuple -> Uni.join().all(
                        roleRepository.trash(tuple.getItem1().id),
                        roleRepository.trash(tuple.getItem2().id))
                        .andCollectFailures()
                        .replaceWith(tuple))
                .chain(tuple -> roleRepository.deleteAllDeleted()
                        .chain(result -> {
                            assertThat(result).isTrue();
                            return Uni.join().all(
                                    roleRepository.findById(tuple.getItem1().id),
                                    roleRepository.findById(tuple.getItem2().id),
                                    roleRepository.findById(tuple.getItem3().id))
                                    .andCollectFailures()
                                    .replaceWith(tuple);
                        }))
                .invoke(tuple -> {
                    assertThat(tuple.getItem1()).isNull();
                    assertThat(tuple.getItem2()).isNull();
                    assertThat(tuple.getItem3()).isNotNull();
                })
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testFindActiveRoles() {
        return Uni.combine().all()
                .unis(createAndPersistRole("ActiveRole1"),
                        createAndPersistRole("TrashedRole1"))
                .asTuple()
                .chain(tuple -> roleRepository.trash(tuple.getItem2().id)
                        .replaceWith(tuple))
                .chain(tuple -> {
                    FindAllRoles req = new FindAllRoles();
                    req.setPage(1);
                    req.setPageSize(10);
                    return roleRepository.findActiveRoles(req);
                })
                .invoke(result -> {
                    assertThat(result.getData()).hasSize(1);
                    assertThat(result.getData().get(0).getRoleName()).isEqualTo("ActiveRole1");
                    assertThat(result.getTotalRecords()).isEqualTo(1);
                })
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testFindTrashedRoles() {
        return Uni.combine().all()
                .unis(createAndPersistRole("TrashedRole2"),
                        createAndPersistRole("ActiveRole2"))
                .asTuple()
                .chain(tuple -> roleRepository.trash(tuple.getItem1().id)
                        .replaceWith(tuple))
                .chain(tuple -> {
                    FindAllRoles req = new FindAllRoles();
                    req.setPage(1);
                    req.setPageSize(10);
                    return roleRepository.findTrashedRoles(req);
                })
                .invoke(result -> {
                    assertThat(result.getData()).hasSize(1);
                    assertThat(result.getData().get(0).getRoleName()).isEqualTo("TrashedRole2");
                })
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testFindRolesWithSearchKeyword() {
        return Uni.join().all(
                createAndPersistRole("SuperAdmin"),
                createAndPersistRole("SuperEditor"),
                createAndPersistRole("Viewer"))
                .andCollectFailures()
                .chain(ignored -> {
                    FindAllRoles req = new FindAllRoles();
                    req.setPage(1);
                    req.setPageSize(10);
                    req.setSearch("Super");
                    return roleRepository.findRoles(req);
                })
                .invoke(result -> {
                    assertThat(result.getData()).hasSize(2);
                    assertThat(result.getTotalRecords()).isEqualTo(2);
                })
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testFindRolesWithPagination() {
        return Uni.join().all(
                createAndPersistRole("PageRole1"),
                createAndPersistRole("PageRole2"),
                createAndPersistRole("PageRole3"),
                createAndPersistRole("PageRole4"),
                createAndPersistRole("PageRole5"))
                .andCollectFailures()
                .chain(ignored -> {
                    FindAllRoles reqPage1 = new FindAllRoles();
                    reqPage1.setPage(1);
                    reqPage1.setPageSize(2);
                    return roleRepository.findRoles(reqPage1);
                })
                .invoke(page1 -> {
                    assertThat(page1.getData()).hasSize(2);
                    assertThat(page1.getTotalRecords()).isEqualTo(5);
                })
                .chain(page1 -> {
                    FindAllRoles reqPage2 = new FindAllRoles();
                    reqPage2.setPage(2);
                    reqPage2.setPageSize(2);
                    return roleRepository.findRoles(reqPage2)
                            .invoke(page2 -> {
                                assertThat(page2.getData()).hasSize(2);
                                assertThat(page2.getData().get(0).getRoleName()).isNotIn(
                                        page1.getData().get(0).getRoleName(),
                                        page1.getData().get(1).getRoleName());
                            });
                })
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testFindRolesWithNullSearchReturnsAll() {
        return Uni.join().all(
                createAndPersistRole("RoleC"),
                createAndPersistRole("RoleD"))
                .andCollectFailures()
                .chain(ignored -> {
                    FindAllRoles req = new FindAllRoles();
                    req.setPage(1);
                    req.setPageSize(10);
                    req.setSearch(null);
                    return roleRepository.findRoles(req);
                })
                .invoke(result -> assertThat(result.getData()).hasSize(2))
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testFindRolesSearchCaseInsensitive() {
        return createAndPersistRole("ManagerRole")
                .chain(ignored -> {
                    FindAllRoles reqLower = new FindAllRoles();
                    reqLower.setPage(1);
                    reqLower.setPageSize(10);
                    reqLower.setSearch("manager");
                    return roleRepository.findRoles(reqLower);
                })
                .invoke(result -> {
                    assertThat(result.getData()).hasSize(1);
                    assertThat(result.getData().get(0).getRoleName()).isEqualTo("ManagerRole");
                })
                .replaceWithVoid();
    }
}
