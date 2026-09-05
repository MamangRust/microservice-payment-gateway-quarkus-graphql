package com.sanedge.gateway.service;

import com.sanedge.gateway.dto.MerchantDocumentDto.*;
import io.smallrye.mutiny.Uni;

public interface MerchantDocumentService {
    Uni<FindAllMerchantDocumentsResponse> listMerchantDocuments(int page, int size, String search);
    Uni<FindAllMerchantDocumentsResponse> listActiveMerchantDocuments(int page, int size, String search);
    Uni<FindAllMerchantDocumentsResponse> listTrashedMerchantDocuments(int page, int size, String search);
    Uni<FindByIdMerchantDocumentResponse> getMerchantDocument(int id);
    Uni<CreateMerchantDocumentResponse> createMerchantDocument(CreateMerchantDocumentBody body);
    Uni<UpdateMerchantDocumentResponse> updateMerchantDocument(int id, UpdateMerchantDocumentBody body);
    Uni<UpdateMerchantDocumentResponse> updateMerchantDocumentStatus(int id, UpdateMerchantDocumentStatusBody body);
    Uni<TrashedMerchantDocumentResponse> deleteMerchantDocument(int id);
    Uni<TrashedMerchantDocumentResponse> trashMerchantDocument(int id);
    Uni<TrashedMerchantDocumentResponse> restoreMerchantDocument(int id);
    Uni<SimpleStatusMessageResponse> deleteMerchantDocumentPermanent(int id);
    Uni<SimpleStatusMessageResponse> restoreAllMerchantDocuments();
    Uni<SimpleStatusMessageResponse> deleteAllMerchantDocuments();
}
