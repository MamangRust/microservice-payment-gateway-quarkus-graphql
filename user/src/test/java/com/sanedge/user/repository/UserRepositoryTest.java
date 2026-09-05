package com.sanedge.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sanedge.user.domain.requests.FindAllUsers;
import com.sanedge.user.entity.User;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

@QuarkusTest
@RunOnVertxContext
class UserRepositoryTest {

        @Inject
        UserRepository userRepository;

        private Uni<User> createAndPersistUser(String username, String email) {
                User user = new User();
                user.setUsername(username);
                user.setFirstname("First_" + username);
                user.setLastname("Last_" + username);
                user.setEmail(email);
                user.setPassword("password123");
                return userRepository.persist(user).replaceWith(user);
        }

        @Test
        @WithSession
        Uni<Void> testCreateAndFindById() {
                return createAndPersistUser("johndoe", "john.doe@example.com")
                                .invoke(saved -> {
                                        assertThat(saved).isNotNull();
                                        assertThat(saved.id).isNotNull();
                                        assertThat(saved.getEmail()).isEqualTo("john.doe@example.com");
                                })
                                .chain(saved -> userRepository.findById(Math.toIntExact(saved.id)))
                                .invoke(found -> {
                                        assertThat(found).isNotNull();
                                        assertThat(found.getUsername()).isEqualTo("johndoe");
                                        assertThat(found.getFirstname()).isEqualTo("First_johndoe");
                                })
                                .replaceWithVoid();
        }

        @Test
        @WithSession
        Uni<Void> testFindByEmail() {
                return createAndPersistUser("janedoe", "jane.doe@example.com")
                                .chain(ignored -> userRepository.findByEmail("jane.doe@example.com"))
                                .invoke(found -> {
                                        assertThat(found).isNotNull();
                                        assertThat(found.getUsername()).isEqualTo("janedoe");
                                })
                                .replaceWithVoid();
        }

        @Test
        @WithSession
        Uni<Void> testFindByEmailReturnsNullWhenNotFound() {
                return userRepository.findByEmail("nonexistent@example.com")
                                .invoke(notFound -> assertThat(notFound).isNull())
                                .replaceWithVoid();
        }

        @Test
        @WithSession
        Uni<Void> testFindByIdReturnsNullWhenNotFound() {
                return userRepository.findById(Integer.MAX_VALUE)
                                .invoke(notFound -> assertThat(notFound).isNull())
                                .replaceWithVoid();
        }

        @Test
        @WithSession
        Uni<Void> testFindByUsername() {
                return createAndPersistUser("alice", "alice@example.com")
                                .chain(ignored -> userRepository.findByUsername("alice"))
                                .invoke(found -> {
                                        assertThat(found).isNotNull();
                                        assertThat(found.getEmail()).isEqualTo("alice@example.com");
                                })
                                .replaceWithVoid();
        }

        @Test
        @WithSession
        Uni<Void> testExistsByUsernameAndEmail() {
                return createAndPersistUser("bob", "bob@example.com")
                                .chain(ignored -> Uni.join().all(
                                                userRepository.existsByUsername("bob"),
                                                userRepository.existsByEmail("bob@example.com"),
                                                userRepository.existsByUsername("notbob"),
                                                userRepository.existsByEmail("notbob@example.com"))
                                                .andCollectFailures())
                                .invoke(results -> {
                                        assertThat(results.get(0)).isTrue();
                                        assertThat(results.get(1)).isTrue();
                                        assertThat(results.get(2)).isFalse();
                                        assertThat(results.get(3)).isFalse();
                                })
                                .replaceWithVoid();
        }

        @Test
        @WithSession
        Uni<Void> testTrashUser() {
                return createAndPersistUser("trashme", "trash@example.com")
                                .invoke(saved -> assertThat(saved.getDeletedAt()).isNull())
                                .chain(saved -> userRepository.trash(saved.id))
                                .invoke(trashed -> {
                                        assertThat(trashed).isNotNull();
                                        assertThat(trashed.getDeletedAt()).isNotNull();
                                })
                                .replaceWithVoid();
        }

        @Test
        @WithSession
        Uni<Void> testTrashUserReturnsNullIfAlreadyTrashed() {
                return createAndPersistUser("trashme2", "trash2@example.com")
                                .chain(saved -> userRepository.trash(saved.id)
                                                .chain(ignored -> userRepository.trash(saved.id)))
                                .invoke(trashedAgain -> assertThat(trashedAgain).isNull())
                                .replaceWithVoid();
        }

        @Test
        @WithSession
        Uni<Void> testTrashUserReturnsNullIfNotFound() {
                return userRepository.trash(99999L)
                                .invoke(trashed -> assertThat(trashed).isNull())
                                .replaceWithVoid();
        }

        @Test
        @WithSession
        Uni<Void> testRestoreUser() {
                return createAndPersistUser("restoreme", "restore@example.com")
                                .chain(saved -> userRepository.trash(saved.id)
                                                .chain(ignored -> userRepository.restore(saved.id)))
                                .invoke(restored -> {
                                        assertThat(restored).isNotNull();
                                        assertThat(restored.getDeletedAt()).isNull();
                                })
                                .replaceWithVoid();
        }

        @Test
        @WithSession
        Uni<Void> testRestoreUserReturnsNullIfNotTrashed() {
                return createAndPersistUser("restoreme2", "restore2@example.com")
                                .chain(saved -> userRepository.restore(saved.id))
                                .invoke(restored -> assertThat(restored).isNull())
                                .replaceWithVoid();
        }

        @Test
        @WithSession
        Uni<Void> testDeletePermanent() {
                return createAndPersistUser("deleteme", "delete@example.com")
                                .chain(saved -> userRepository.trash(saved.id)
                                                .chain(ignored -> userRepository.deletePermanent(saved.id))
                                                .chain(deleted -> {
                                                        assertThat(deleted).isNotNull();
                                                        return userRepository.findById(Math.toIntExact(saved.id));
                                                }))
                                .invoke(checkDb -> assertThat(checkDb).isNull())
                                .replaceWithVoid();
        }

        @Test
        @WithSession
        Uni<Void> testDeletePermanentFailsIfNotTrashed() {
                return createAndPersistUser("deleteme2", "delete2@example.com")
                                .chain(saved -> userRepository.deletePermanent(saved.id)
                                                .chain(deleted -> {
                                                        assertThat(deleted).isNull();
                                                        return userRepository.findById(Math.toIntExact(saved.id));
                                                }))
                                .invoke(checkDb -> assertThat(checkDb).isNotNull())
                                .replaceWithVoid();
        }

        @Test
        @WithSession
        Uni<Void> testRestoreAllDeleted() {
                return Uni.combine().all()
                                .unis(createAndPersistUser("bulk1", "bulk1@example.com"),
                                                createAndPersistUser("bulk2", "bulk2@example.com"))
                                .asTuple()
                                .chain(tuple -> Uni.join().all(
                                                userRepository.trash(tuple.getItem1().id),
                                                userRepository.trash(tuple.getItem2().id))
                                                .andCollectFailures()
                                                .replaceWith(tuple))
                                .chain(tuple -> userRepository.restoreAllDeleted()
                                                .chain(result -> {
                                                        assertThat(result).isTrue();
                                                        return Uni.join().all(
                                                                        userRepository.findById(Math.toIntExact(
                                                                                        tuple.getItem1().id)),
                                                                        userRepository.findById(Math.toIntExact(
                                                                                        tuple.getItem2().id)))
                                                                        .andCollectFailures()
                                                                        .replaceWith(tuple);
                                                }))
                                .invoke(tuple -> {
                                        assertThat(tuple.getItem1().getDeletedAt()).isNull();
                                        assertThat(tuple.getItem2().getDeletedAt()).isNull();
                                })
                                .replaceWithVoid();
        }

        @Test
        @WithSession
        Uni<Void> testDeleteAllDeleted() {
                return Uni.combine().all()
                                .unis(createAndPersistUser("bulkdel1", "bulkdel1@example.com"),
                                                createAndPersistUser("bulkdel2", "bulkdel2@example.com"),
                                                createAndPersistUser("bulkdel3", "bulkdel3@example.com"))
                                .asTuple()
                                .chain(tuple -> Uni.join().all(
                                                userRepository.trash(tuple.getItem1().id),
                                                userRepository.trash(tuple.getItem2().id))
                                                .andCollectFailures()
                                                .replaceWith(tuple))
                                .chain(tuple -> userRepository.deleteAllDeleted()
                                                .chain(result -> {
                                                        assertThat(result).isTrue();
                                                        return Uni.join().all(
                                                                        userRepository.findById(Math.toIntExact(
                                                                                        tuple.getItem1().id)),
                                                                        userRepository.findById(Math.toIntExact(
                                                                                        tuple.getItem2().id)),
                                                                        userRepository.findById(Math.toIntExact(
                                                                                        tuple.getItem3().id)))
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
        Uni<Void> testFindActiveUsers() {
                return Uni.combine().all()
                                .unis(createAndPersistUser("active1", "active1@example.com"),
                                                createAndPersistUser("trashed1", "trashed1@example.com"))
                                .asTuple()
                                .chain(tuple -> userRepository.trash(tuple.getItem2().id)
                                                .replaceWith(tuple))
                                .chain(tuple -> {
                                        FindAllUsers req = new FindAllUsers();
                                        req.setPage(1);
                                        req.setPageSize(10);
                                        return userRepository.findActiveUsers(req);
                                })
                                .invoke(result -> {
                                        assertThat(result.getData()).hasSize(1);
                                        assertThat(result.getData().get(0).getUsername()).isEqualTo("active1");
                                        assertThat(result.getTotalRecords()).isEqualTo(1);
                                })
                                .replaceWithVoid();
        }

        @Test
        @WithSession
        Uni<Void> testFindTrashedUsers() {
                return Uni.combine().all()
                                .unis(createAndPersistUser("trashed2", "trashed2@example.com"),
                                                createAndPersistUser("active2", "active2@example.com"))
                                .asTuple()
                                .chain(tuple -> userRepository.trash(tuple.getItem1().id)
                                                .replaceWith(tuple))
                                .chain(tuple -> {
                                        FindAllUsers req = new FindAllUsers();
                                        req.setPage(1);
                                        req.setPageSize(10);
                                        return userRepository.findTrashedUsers(req);
                                })
                                .invoke(result -> {
                                        assertThat(result.getData()).hasSize(1);
                                        assertThat(result.getData().get(0).getUsername()).isEqualTo("trashed2");
                                })
                                .replaceWithVoid();
        }

        @Test
        @WithSession
        Uni<Void> testFindUsersWithSearchKeyword() {
                return Uni.join().all(
                                createAndPersistUser("superman", "superman@dc.com"),
                                createAndPersistUser("spiderman", "spiderman@marvel.com"),
                                createAndPersistUser("batman", "batman@dc.com"))
                                .andCollectFailures()
                                .chain(ignored -> {
                                        FindAllUsers req = new FindAllUsers();
                                        req.setPage(1);
                                        req.setPageSize(10);
                                        req.setSearch("man");
                                        return userRepository.findUsers(req);
                                })
                                .invoke(result -> {
                                        assertThat(result.getData()).hasSize(2);
                                        assertThat(result.getTotalRecords()).isEqualTo(2);
                                })
                                .replaceWithVoid();
        }

        @Test
        @WithSession
        Uni<Void> testFindUsersWithPagination() {
                return Uni.join().all(
                                createAndPersistUser("pageuser1", "pageuser1@example.com"),
                                createAndPersistUser("pageuser2", "pageuser2@example.com"),
                                createAndPersistUser("pageuser3", "pageuser3@example.com"),
                                createAndPersistUser("pageuser4", "pageuser4@example.com"),
                                createAndPersistUser("pageuser5", "pageuser5@example.com"))
                                .andCollectFailures()
                                .chain(ignored -> {
                                        FindAllUsers reqPage1 = new FindAllUsers();
                                        reqPage1.setPage(1);
                                        reqPage1.setPageSize(2);
                                        return userRepository.findUsers(reqPage1);
                                })
                                .invoke(page1 -> {
                                        assertThat(page1.getData()).hasSize(2);
                                        assertThat(page1.getTotalRecords()).isEqualTo(5);
                                })
                                .chain(page1 -> {
                                        FindAllUsers reqPage2 = new FindAllUsers();
                                        reqPage2.setPage(2);
                                        reqPage2.setPageSize(2);
                                        return userRepository.findUsers(reqPage2)
                                                        .invoke(page2 -> {
                                                                assertThat(page2.getData()).hasSize(2);
                                                                assertThat(page2.getData().get(0).getUsername())
                                                                                .isNotIn(
                                                                                                page1.getData().get(0)
                                                                                                                .getUsername(),
                                                                                                page1.getData().get(1)
                                                                                                                .getUsername());
                                                        });
                                })
                                .replaceWithVoid();
        }
}
