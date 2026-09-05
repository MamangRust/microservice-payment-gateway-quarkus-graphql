package com.sanedge.gateway.service.impl;

import com.sanedge.gateway.dto.MerchantDocumentDto.*;
import com.sanedge.gateway.service.MerchantDocumentService;
import com.sanedge.gateway.telemetry.TelemetryHelper;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class MerchantDocumentServiceImpl implements MerchantDocumentService {

    private static final Logger LOG = Logger.getLogger(MerchantDocumentServiceImpl.class);

    @Inject
    TelemetryHelper telemetryHelper;

    @GrpcClient("merchant")
    pb.merchant_document.MutinyMerchantDocumentQueryServiceGrpc.MutinyMerchantDocumentQueryServiceStub merchantDocumentQueryService;

    @GrpcClient("merchant")
    pb.merchant_document.MutinyMerchantDocumentCommandServiceGrpc.MutinyMerchantDocumentCommandServiceStub merchantDocumentCommandService;

    @Override
    public Uni<FindAllMerchantDocumentsResponse> listMerchantDocuments(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("merchantDocument.listMerchantDocuments", () -> merchantDocumentQueryService.findAll(
                pb.merchant_document.MerchantDocumentOuterClass.FindAllMerchantDocumentsRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(FindAllMerchantDocumentsResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to list merchant documents: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<FindAllMerchantDocumentsResponse> listActiveMerchantDocuments(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("merchantDocument.listActiveMerchantDocuments", () -> merchantDocumentQueryService.findAllActive(
                pb.merchant_document.MerchantDocumentOuterClass.FindAllMerchantDocumentsRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(FindAllMerchantDocumentsResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to list active merchant documents: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<FindAllMerchantDocumentsResponse> listTrashedMerchantDocuments(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("merchantDocument.listTrashedMerchantDocuments", () -> merchantDocumentQueryService.findAllTrashed(
                pb.merchant_document.MerchantDocumentOuterClass.FindAllMerchantDocumentsRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(FindAllMerchantDocumentsResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to list trashed merchant documents: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<FindByIdMerchantDocumentResponse> getMerchantDocument(int id) {
        return telemetryHelper.traceAndMetric("merchantDocument.getMerchantDocument", () -> merchantDocumentQueryService.findById(
                pb.merchant_document.MerchantDocumentOuterClass.FindMerchantDocumentByIdRequest.newBuilder()
                        .setDocumentId(id)
                        .build())
                .map(FindByIdMerchantDocumentResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get merchant document: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CreateMerchantDocumentResponse> createMerchantDocument(CreateMerchantDocumentBody body) {
        pb.merchant_document.MerchantDocumentCommand.CreateMerchantDocumentRequest req = pb.merchant_document.MerchantDocumentCommand.CreateMerchantDocumentRequest.newBuilder()
                .setMerchantId(body.merchantId())
                .setDocumentType(body.documentType() == null ? "" : body.documentType())
                .setDocumentUrl(body.documentUrl() == null ? "" : body.documentUrl())
                .build();
        return telemetryHelper.traceAndMetric("merchantDocument.createMerchantDocument", () -> merchantDocumentCommandService.create(req)
                .map(CreateMerchantDocumentResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to create merchant document: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<UpdateMerchantDocumentResponse> updateMerchantDocument(int id, UpdateMerchantDocumentBody body) {
        pb.merchant_document.MerchantDocumentCommand.UpdateMerchantDocumentRequest req = pb.merchant_document.MerchantDocumentCommand.UpdateMerchantDocumentRequest.newBuilder()
                .setDocumentId(id)
                .setMerchantId(body.merchantId())
                .setDocumentType(body.documentType() == null ? "" : body.documentType())
                .setDocumentUrl(body.documentUrl() == null ? "" : body.documentUrl())
                .setNote(body.note() == null ? "" : body.note())
                .setStatus(body.status() == null ? "" : body.status())
                .build();
        return telemetryHelper.traceAndMetric("merchantDocument.updateMerchantDocument", () -> merchantDocumentCommandService.update(req)
                .map(UpdateMerchantDocumentResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to update merchant document: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<UpdateMerchantDocumentResponse> updateMerchantDocumentStatus(int id, UpdateMerchantDocumentStatusBody body) {
        pb.merchant_document.MerchantDocumentCommand.UpdateMerchantDocumentStatusRequest req = pb.merchant_document.MerchantDocumentCommand.UpdateMerchantDocumentStatusRequest.newBuilder()
                .setDocumentId(id)
                .setMerchantId(body.merchantId())
                .setNote(body.note() == null ? "" : body.note())
                .setStatus(body.status() == null ? "" : body.status())
                .build();
        return telemetryHelper.traceAndMetric("merchantDocument.updateMerchantDocumentStatus", () -> merchantDocumentCommandService.updateStatus(req)
                .map(UpdateMerchantDocumentResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to update merchant document status: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<TrashedMerchantDocumentResponse> deleteMerchantDocument(int id) {
        return telemetryHelper.traceAndMetric("merchantDocument.deleteMerchantDocument", () -> merchantDocumentCommandService.trashed(
                pb.merchant_document.MerchantDocumentOuterClass.FindMerchantDocumentByIdRequest.newBuilder()
                        .setDocumentId(id)
                        .build())
                .map(TrashedMerchantDocumentResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to delete merchant document: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<TrashedMerchantDocumentResponse> trashMerchantDocument(int id) {
        return telemetryHelper.traceAndMetric("merchantDocument.trashMerchantDocument", () -> merchantDocumentCommandService.trashed(
                pb.merchant_document.MerchantDocumentOuterClass.FindMerchantDocumentByIdRequest.newBuilder()
                        .setDocumentId(id)
                        .build())
                .map(TrashedMerchantDocumentResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to trash merchant document: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<TrashedMerchantDocumentResponse> restoreMerchantDocument(int id) {
        return telemetryHelper.traceAndMetric("merchantDocument.restoreMerchantDocument", () -> merchantDocumentCommandService.restore(
                pb.merchant_document.MerchantDocumentOuterClass.FindMerchantDocumentByIdRequest.newBuilder()
                        .setDocumentId(id)
                        .build())
                .map(TrashedMerchantDocumentResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore merchant document: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<SimpleStatusMessageResponse> deleteMerchantDocumentPermanent(int id) {
        return telemetryHelper.traceAndMetric("merchantDocument.deleteMerchantDocumentPermanent", () -> merchantDocumentCommandService.deletePermanent(
                pb.merchant_document.MerchantDocumentOuterClass.FindMerchantDocumentByIdRequest.newBuilder()
                        .setDocumentId(id)
                        .build())
                .map(SimpleStatusMessageResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to permanently delete merchant document: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<SimpleStatusMessageResponse> restoreAllMerchantDocuments() {
        return telemetryHelper.traceAndMetric("merchantDocument.restoreAllMerchantDocuments", () -> merchantDocumentCommandService.restoreAll(com.google.protobuf.Empty.getDefaultInstance())
                .map(SimpleStatusMessageResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore all merchant documents: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<SimpleStatusMessageResponse> deleteAllMerchantDocuments() {
        return telemetryHelper.traceAndMetric("merchantDocument.deleteAllMerchantDocuments", () -> merchantDocumentCommandService.deleteAllPermanent(com.google.protobuf.Empty.getDefaultInstance())
                .map(SimpleStatusMessageResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to delete all merchant documents permanently: " + throwable.getMessage(), throwable)));
    }
}
