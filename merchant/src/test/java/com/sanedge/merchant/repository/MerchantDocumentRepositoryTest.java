package com.sanedge.merchant.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.sanedge.merchant.domain.requests.FindAllMerchantDocuments;
import com.sanedge.merchant.entity.MerchantDocument;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

@QuarkusTest
@RunOnVertxContext
class MerchantDocumentRepositoryTest {

    @Inject
    MerchantDocumentQueryRepository documentQueryRepo;

    @Inject
    MerchantDocumentCommandRepository documentCommandRepo;

    private Uni<Long> persist(Long merchantId, String docType, String docUrl) {
        MerchantDocument doc = new MerchantDocument();
        doc.setMerchantId(merchantId.intValue());
        doc.setDocumentType(docType);
        doc.setDocumentUrl(docUrl);
        doc.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
        doc.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
        return documentQueryRepo.persist(doc).map(d -> d.getDocumentId());
    }

    private Uni<Long> persist(Long merchantId) {
        return persist(merchantId, "ID_CARD", "http://docs.com/id.jpg");
    }

    private Uni<Void> clean() {
        return documentQueryRepo.deleteAll().replaceWithVoid();
    }

    private FindAllMerchantDocuments findAllReq(int page, int size, String search) {
        FindAllMerchantDocuments r = new FindAllMerchantDocuments();
        r.setPage(page);
        r.setPageSize(size);
        r.setSearch(search == null ? "" : search);
        return r;
    }

    @Test
    @WithTransaction
    Uni<Void> testCreateAndFindById() {
        return clean()
                .chain(() -> persist(1L, "PASSPORT", "http://passport.img"))
                .chain(id -> documentQueryRepo.findById(id))
                .invoke(found -> {
                    assertThat(found).isNotNull();
                    assertThat(found.getDocumentType()).isEqualTo("PASSPORT");
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testTrashDocument() {
        return clean()
                .chain(() -> persist(100L))
                .chain(id -> documentCommandRepo.trashed(id))
                .invoke(t -> assertThat(t.getDeletedAt()).isNotNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testRestoreDocument() {
        return clean()
                .chain(() -> persist(200L))
                .chain(id -> documentCommandRepo.trashed(id)
                        .chain(() -> documentCommandRepo.restore(id)))
                .invoke(r -> assertThat(r.getDeletedAt()).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testDeletePermanent() {
        return clean()
                .chain(() -> persist(300L))
                .chain(id -> documentCommandRepo.trashed(id)
                        .chain(() -> documentCommandRepo.deletePermanent(id))
                        .chain(deleted -> documentQueryRepo.findById(id)))
                .invoke(n -> assertThat(n).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testRestoreAllDeleted() {
        return clean()
                .chain(() -> persist(1L).chain(id -> documentCommandRepo.trashed(id).replaceWithVoid()))
                .chain(() -> persist(2L).chain(id -> documentCommandRepo.trashed(id).replaceWithVoid()))
                .chain(() -> persist(3L))
                .chain(() -> documentCommandRepo.restoreAllDeleted())
                .invoke(r -> assertThat(r).isTrue())
                .chain(() -> documentQueryRepo.findTrashedDocuments(findAllReq(1, 10, "")))
                .invoke(r -> assertThat(r.getTotalRecords()).isZero())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindDocumentsWithSearch() {
        return clean()
                .chain(() -> persist(1L, "ID_CARD", "id1.jpg"))
                .chain(() -> persist(1L, "BUSINESS_LICENSE", "license.pdf"))
                .chain(() -> documentQueryRepo.findDocuments(findAllReq(1, 10, "BUSINESS")))
                .invoke(r -> {
                    assertThat(r.getTotalRecords()).isEqualTo(1);
                    assertThat(r.getData().get(0).getDocumentType()).isEqualTo("BUSINESS_LICENSE");
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testPagination() {
        return clean()
                .chain(() -> persist(10L))
                .chain(() -> persist(10L))
                .chain(() -> persist(10L))
                .chain(() -> persist(10L))
                .chain(() -> documentQueryRepo.findDocuments(findAllReq(1, 2, "")))
                .invoke(page1 -> {
                    assertThat(page1.getData()).hasSize(2);
                    assertThat(page1.getTotalRecords()).isEqualTo(4);
                })
                .replaceWithVoid();
    }
}