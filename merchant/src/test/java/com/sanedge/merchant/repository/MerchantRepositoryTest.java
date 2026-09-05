package com.sanedge.merchant.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.sanedge.common.enums.Status;
import com.sanedge.merchant.domain.requests.FindAllMerchants;
import com.sanedge.merchant.entity.Merchant;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

@QuarkusTest
@RunOnVertxContext
class MerchantRepositoryTest {

    @Inject
    MerchantQueryRepository merchantQueryRepo;

    @Inject
    MerchantCommandRepository merchantCommandRepo;

    private Uni<Long> persist(String name, String apiKey, String status) {
        Merchant merchant = new Merchant();
        merchant.setName(name);
        merchant.setApiKey(apiKey);
        merchant.setStatus(Status.valueOf(status));
        merchant.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
        merchant.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
        return merchantQueryRepo.persist(merchant).map(m -> m.getMerchantId());
    }

    private Uni<Long> persist(String name, String apiKey) {
        return persist(name, apiKey, "SUCCESS");
    }

    private Uni<Void> clean() {
        return merchantQueryRepo.deleteAll().replaceWithVoid();
    }

    private FindAllMerchants findAllReq(int page, int size, String search) {
        FindAllMerchants r = new FindAllMerchants();
        r.setPage(page);
        r.setPageSize(size);
        r.setSearch(search == null ? "" : search);
        return r;
    }

    @Test
    @WithTransaction
    Uni<Void> testCreateAndFindById() {
        return clean()
                .chain(() -> persist("Merchant One", "key-001"))
                .chain(id -> merchantQueryRepo.findById(id))
                .invoke(found -> {
                    assertThat(found).isNotNull();
                    assertThat(found.getName()).isEqualTo("Merchant One");
                    assertThat(found.getApiKey()).isEqualTo("key-001");
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindByIdReturnsNullWhenNotFound() {
        return clean()
                .chain(() -> merchantQueryRepo.findById(99999L))
                .invoke(n -> assertThat(n).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindByApiKey() {
        return clean()
                .chain(() -> persist("Merchant Key", "unique-key"))
                .chain(() -> merchantQueryRepo.findByApiKey("unique-key"))
                .invoke(found -> {
                    assertThat(found).isNotNull();
                    assertThat(found.getName()).isEqualTo("Merchant Key");
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testTrashMerchant() {
        return clean()
                .chain(() -> persist("Trash Me", "trash-key"))
                .chain(id -> merchantCommandRepo.trashed(id))
                .invoke(t -> {
                    assertThat(t).isNotNull();
                    assertThat(t.getDeletedAt()).isNotNull();
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testRestoreMerchant() {
        return clean()
                .chain(() -> persist("Restore Me", "restore-key"))
                .chain(id -> merchantCommandRepo.trashed(id)
                        .chain(() -> merchantCommandRepo.restore(id)))
                .invoke(r -> {
                    assertThat(r).isNotNull();
                    assertThat(r.getDeletedAt()).isNull();
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testDeletePermanent() {
        return clean()
                .chain(() -> persist("Delete Perm", "del-key"))
                .chain(id -> merchantCommandRepo.trashed(id)
                        .chain(() -> merchantCommandRepo.deletePermanent(id))
                        .chain(deleted -> merchantQueryRepo.findById(id)))
                .invoke(n -> assertThat(n).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testRestoreAllDeleted() {
        return clean()
                .chain(() -> persist("Bulk Restore 1", "bulk1")
                        .chain(id -> merchantCommandRepo.trashed(id).replaceWithVoid()))
                .chain(() -> persist("Bulk Restore 2", "bulk2")
                        .chain(id -> merchantCommandRepo.trashed(id).replaceWithVoid()))
                .chain(() -> persist("Active One", "active1"))
                .chain(() -> merchantCommandRepo.restoreAllDeleted())
                .invoke(r -> assertThat(r).isTrue())
                .chain(() -> merchantQueryRepo.findTrashedMerchants(findAllReq(1, 10, "")))
                .invoke(r -> assertThat(r.getTotalRecords()).isZero())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testDeleteAllDeleted() {
        return clean()
                .chain(() -> persist("Delete All 1", "del1")
                        .chain(id -> merchantCommandRepo.trashed(id).replaceWithVoid()))
                .chain(() -> persist("Delete All 2", "del2")
                        .chain(id -> merchantCommandRepo.trashed(id).replaceWithVoid()))
                .chain(() -> persist("Keep", "keep"))
                .chain(() -> merchantCommandRepo.deleteAllDeleted())
                .invoke(r -> assertThat(r).isTrue())
                .chain(() -> merchantQueryRepo.findMerchants(findAllReq(1, 10, "")))
                .invoke(r -> assertThat(r.getTotalRecords()).isEqualTo(1))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindMerchantsWithSearch() {
        return clean()
                .chain(() -> persist("Alpha Store", "a-key"))
                .chain(() -> persist("Beta Shop", "b-key"))
                .chain(() -> persist("Gamma Market", "c-key"))
                .chain(() -> merchantQueryRepo.findMerchants(findAllReq(1, 10, "beta")))
                .invoke(r -> {
                    assertThat(r.getTotalRecords()).isEqualTo(1);
                    assertThat(r.getData().get(0).getName()).isEqualTo("Beta Shop");
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testPagination() {
        return clean()
                .chain(() -> persist("Page1", "p1"))
                .chain(() -> persist("Page2", "p2"))
                .chain(() -> persist("Page3", "p3"))
                .chain(() -> persist("Page4", "p4"))
                .chain(() -> persist("Page5", "p5"))
                .chain(() -> merchantQueryRepo.findMerchants(findAllReq(1, 2, "")))
                .invoke(page1 -> {
                    assertThat(page1.getData()).hasSize(2);
                    assertThat(page1.getTotalRecords()).isEqualTo(5);
                })
                .chain(() -> merchantQueryRepo.findMerchants(findAllReq(3, 2, "")))
                .invoke(page3 -> assertThat(page3.getData()).hasSize(1))
                .replaceWithVoid();
    }
}