package com.sanedge.gateway.resource;

import com.sanedge.gateway.dto.MerchantDocumentDto.*;
import com.sanedge.gateway.service.MerchantDocumentService;
import io.smallrye.mutiny.Uni;
import com.sanedge.gateway.exception.GraphQLErrorHandler;
import org.eclipse.microprofile.graphql.*;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@GraphQLApi
@Singleton
@GraphQLErrorHandler
public class MerchantDocumentResource {

    @Inject
    MerchantDocumentService merchantDocumentService;

    @Query("listMerchantDocuments")
    @Description("List all merchant documents")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<FindAllMerchantDocumentsResponse> listMerchantDocuments(
            @Name("page") @DefaultValue("1") int page,
            @Name("size") @DefaultValue("20") int size,
            @Name("search") String search) {
        return merchantDocumentService.listMerchantDocuments(page, size, search);
    }

    @Query("listActiveMerchantDocuments")
    @Description("List all active merchant documents")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<FindAllMerchantDocumentsResponse> listActiveMerchantDocuments(
            @Name("page") @DefaultValue("1") int page,
            @Name("size") @DefaultValue("20") int size,
            @Name("search") String search) {
        return merchantDocumentService.listActiveMerchantDocuments(page, size, search);
    }

    @Query("listTrashedMerchantDocuments")
    @Description("List all trashed merchant documents")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<FindAllMerchantDocumentsResponse> listTrashedMerchantDocuments(
            @Name("page") @DefaultValue("1") int page,
            @Name("size") @DefaultValue("20") int size,
            @Name("search") String search) {
        return merchantDocumentService.listTrashedMerchantDocuments(page, size, search);
    }

    @Query("getMerchantDocument")
    @Description("Get merchant document by ID")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<FindByIdMerchantDocumentResponse> getMerchantDocument(@Name("id") int id) {
        return merchantDocumentService.getMerchantDocument(id);
    }

    @Mutation("createMerchantDocument")
    @Description("Create a new merchant document")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<CreateMerchantDocumentResponse> createMerchantDocument(@Name("body") CreateMerchantDocumentBody body) {
        return merchantDocumentService.createMerchantDocument(body);
    }

    @Mutation("updateMerchantDocument")
    @Description("Update merchant document")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<UpdateMerchantDocumentResponse> updateMerchantDocument(@Name("id") int id, @Name("body") UpdateMerchantDocumentBody body) {
        return merchantDocumentService.updateMerchantDocument(id, body);
    }

    @Mutation("updateMerchantDocumentStatus")
    @Description("Update merchant document status")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<UpdateMerchantDocumentResponse> updateMerchantDocumentStatus(@Name("id") int id, @Name("body") UpdateMerchantDocumentStatusBody body) {
        return merchantDocumentService.updateMerchantDocumentStatus(id, body);
    }

    @Mutation("deleteMerchantDocument")
    @Description("Soft-delete a merchant document")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<TrashedMerchantDocumentResponse> deleteMerchantDocument(@Name("id") int id) {
        return merchantDocumentService.deleteMerchantDocument(id);
    }

    @Mutation("trashMerchantDocument")
    @Description("Soft-delete merchant document by ID")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<TrashedMerchantDocumentResponse> trashMerchantDocument(@Name("id") int id) {
        return merchantDocumentService.trashMerchantDocument(id);
    }

    @Mutation("restoreMerchantDocument")
    @Description("Restore merchant document by ID")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<TrashedMerchantDocumentResponse> restoreMerchantDocument(@Name("id") int id) {
        return merchantDocumentService.restoreMerchantDocument(id);
    }

    @Mutation("deleteMerchantDocumentPermanent")
    @Description("Permanently delete a merchant document")
    @RolesAllowed({"ROLE_ADMIN"})
    public Uni<SimpleStatusMessageResponse> deleteMerchantDocumentPermanent(@Name("id") int id) {
        return merchantDocumentService.deleteMerchantDocumentPermanent(id);
    }

    @Mutation("restoreAllMerchantDocuments")
    @Description("Restore all merchant documents")
    @RolesAllowed({"ROLE_ADMIN"})
    public Uni<SimpleStatusMessageResponse> restoreAllMerchantDocuments() {
        return merchantDocumentService.restoreAllMerchantDocuments();
    }

    @Mutation("deleteAllMerchantDocuments")
    @Description("Delete all merchant documents permanently")
    @RolesAllowed({"ROLE_ADMIN"})
    public Uni<SimpleStatusMessageResponse> deleteAllMerchantDocuments() {
        return merchantDocumentService.deleteAllMerchantDocuments();
    }
}
