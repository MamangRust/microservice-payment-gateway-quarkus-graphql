package com.sanedge.gateway.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.gateway.dto.MerchantDocumentDto.*;
import com.sanedge.gateway.telemetry.TelemetryHelper;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class MerchantDocumentServiceImplTest {

    @Mock
    TelemetryHelper telemetryHelper;

    @Mock
    pb.merchant_document.MutinyMerchantDocumentQueryServiceGrpc.MutinyMerchantDocumentQueryServiceStub merchantDocumentQueryService;

    @Mock
    pb.merchant_document.MutinyMerchantDocumentCommandServiceGrpc.MutinyMerchantDocumentCommandServiceStub merchantDocumentCommandService;

    MerchantDocumentServiceImpl merchantDocumentService;

    @BeforeEach
    void setUp() throws Exception {
        merchantDocumentService = new MerchantDocumentServiceImpl();

        setField(merchantDocumentService, "telemetryHelper", telemetryHelper);
        setField(merchantDocumentService, "merchantDocumentQueryService", merchantDocumentQueryService);
        setField(merchantDocumentService, "merchantDocumentCommandService", merchantDocumentCommandService);

        lenient().when(telemetryHelper.traceAndMetric(anyString(), any(java.util.function.Supplier.class)))
                .thenAnswer(inv -> {
                    java.util.function.Supplier<Uni<?>> supplier = inv.getArgument(1);
                    return supplier.get();
                });
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void listMerchantDocuments_returnsSuccess() {
        pb.merchant_document.MerchantDocumentOuterClass.MerchantDocument protoDoc = 
                pb.merchant_document.MerchantDocumentOuterClass.MerchantDocument.newBuilder()
                .setDocumentId(1)
                .setMerchantId(1)
                .setDocumentType("LICENSE")
                .setDocumentUrl("https://example.com/doc.pdf")
                .setStatus("PENDING")
                .setUpdatedAt("2024-01-01T00:00:00Z")
                .build();

        pb.merchant_document.MerchantDocumentQuery.ApiResponsePaginationMerchantDocument responseProto = 
                pb.merchant_document.MerchantDocumentQuery.ApiResponsePaginationMerchantDocument.newBuilder()
                .addData(protoDoc)
                .setStatus("success")
                .setMessage("Merchant documents found")
                .build();

        when(merchantDocumentQueryService.findAll(any(pb.merchant_document.MerchantDocumentOuterClass.FindAllMerchantDocumentsRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        FindAllMerchantDocumentsResponse result = merchantDocumentService.listMerchantDocuments(1, 10, "").await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).documentType()).isEqualTo("LICENSE");
    }

    @Test
    void getMerchantDocument_returnsSuccess() {
        pb.merchant_document.MerchantDocumentOuterClass.MerchantDocument protoDoc = 
                pb.merchant_document.MerchantDocumentOuterClass.MerchantDocument.newBuilder()
                .setDocumentId(1)
                .setMerchantId(1)
                .setDocumentType("LICENSE")
                .setDocumentUrl("https://example.com/doc.pdf")
                .setStatus("PENDING")
                .setUpdatedAt("2024-01-01T00:00:00Z")
                .build();

        pb.merchant_document.MerchantDocumentOuterClass.ApiResponseMerchantDocument responseProto = 
                pb.merchant_document.MerchantDocumentOuterClass.ApiResponseMerchantDocument.newBuilder()
                .setData(protoDoc)
                .setStatus("success")
                .setMessage("Merchant document found")
                .build();

        when(merchantDocumentQueryService.findById(any(pb.merchant_document.MerchantDocumentOuterClass.FindMerchantDocumentByIdRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        FindByIdMerchantDocumentResponse result = merchantDocumentService.getMerchantDocument(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNotNull();
        assertThat(result.data().documentId()).isEqualTo(1);
        assertThat(result.data().documentType()).isEqualTo("LICENSE");
    }

    @Test
    void createMerchantDocument_returnsSuccess() {
        pb.merchant_document.MerchantDocumentOuterClass.MerchantDocument protoDoc = 
                pb.merchant_document.MerchantDocumentOuterClass.MerchantDocument.newBuilder()
                .setDocumentId(2)
                .setMerchantId(1)
                .setDocumentType("BUSINESS_PERMIT")
                .setDocumentUrl("https://example.com/permit.pdf")
                .setStatus("PENDING")
                .setUpdatedAt("2024-01-01T00:00:00Z")
                .build();

        pb.merchant_document.MerchantDocumentOuterClass.ApiResponseMerchantDocument responseProto = 
                pb.merchant_document.MerchantDocumentOuterClass.ApiResponseMerchantDocument.newBuilder()
                .setData(protoDoc)
                .setStatus("success")
                .setMessage("Merchant document created")
                .build();

        when(merchantDocumentCommandService.create(any(pb.merchant_document.MerchantDocumentCommand.CreateMerchantDocumentRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        CreateMerchantDocumentBody request = new CreateMerchantDocumentBody(1, "BUSINESS_PERMIT", "https://example.com/permit.pdf");
        CreateMerchantDocumentResponse result = merchantDocumentService.createMerchantDocument(request).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNotNull();
        assertThat(result.data().documentType()).isEqualTo("BUSINESS_PERMIT");
    }

    @Test
    void updateMerchantDocument_returnsSuccess() {
        pb.merchant_document.MerchantDocumentOuterClass.MerchantDocument protoDoc = 
                pb.merchant_document.MerchantDocumentOuterClass.MerchantDocument.newBuilder()
                .setDocumentId(1)
                .setMerchantId(1)
                .setDocumentType("LICENSE")
                .setDocumentUrl("https://example.com/doc-updated.pdf")
                .setNote("Updated")
                .setStatus("APPROVED")
                .setUpdatedAt("2024-01-02T00:00:00Z")
                .build();

        pb.merchant_document.MerchantDocumentOuterClass.ApiResponseMerchantDocument responseProto = 
                pb.merchant_document.MerchantDocumentOuterClass.ApiResponseMerchantDocument.newBuilder()
                .setData(protoDoc)
                .setStatus("success")
                .setMessage("Merchant document updated")
                .build();

        when(merchantDocumentCommandService.update(any(pb.merchant_document.MerchantDocumentCommand.UpdateMerchantDocumentRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        UpdateMerchantDocumentBody request = new UpdateMerchantDocumentBody(1, "LICENSE", "https://example.com/doc-updated.pdf", "Updated", "APPROVED");
        UpdateMerchantDocumentResponse result = merchantDocumentService.updateMerchantDocument(1, request).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNotNull();
        assertThat(result.data().status()).isEqualTo("APPROVED");
    }

    @Test
    void deleteMerchantDocument_returnsSuccess() {
        pb.merchant_document.MerchantDocumentOuterClass.MerchantDocumentDeleteAt protoDoc = 
                pb.merchant_document.MerchantDocumentOuterClass.MerchantDocumentDeleteAt.newBuilder()
                .setDocumentId(1)
                .setMerchantId(1)
                .setDocumentType("LICENSE")
                .setDocumentUrl("https://example.com/doc.pdf")
                .setStatus("PENDING")
                .setUpdatedAt("2024-01-02T00:00:00Z")
                .build();

        pb.merchant_document.MerchantDocumentOuterClass.ApiResponseMerchantDocumentDeleteAt responseProto = 
                pb.merchant_document.MerchantDocumentOuterClass.ApiResponseMerchantDocumentDeleteAt.newBuilder()
                .setData(protoDoc)
                .setStatus("success")
                .setMessage("Merchant document deleted")
                .build();

        when(merchantDocumentCommandService.trashed(any(pb.merchant_document.MerchantDocumentOuterClass.FindMerchantDocumentByIdRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        TrashedMerchantDocumentResponse result = merchantDocumentService.deleteMerchantDocument(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNotNull();
        assertThat(result.data().documentId()).isEqualTo(1);
    }

    @Test
    void restoreMerchantDocument_returnsSuccess() {
        pb.merchant_document.MerchantDocumentOuterClass.MerchantDocumentDeleteAt protoDoc = 
                pb.merchant_document.MerchantDocumentOuterClass.MerchantDocumentDeleteAt.newBuilder()
                .setDocumentId(1)
                .setMerchantId(1)
                .setDocumentType("LICENSE")
                .setDocumentUrl("https://example.com/doc.pdf")
                .setStatus("PENDING")
                .setUpdatedAt("2024-01-02T00:00:00Z")
                .build();

        pb.merchant_document.MerchantDocumentOuterClass.ApiResponseMerchantDocumentDeleteAt responseProto = 
                pb.merchant_document.MerchantDocumentOuterClass.ApiResponseMerchantDocumentDeleteAt.newBuilder()
                .setData(protoDoc)
                .setStatus("success")
                .setMessage("Merchant document restored")
                .build();

        when(merchantDocumentCommandService.restore(any(pb.merchant_document.MerchantDocumentOuterClass.FindMerchantDocumentByIdRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        TrashedMerchantDocumentResponse result = merchantDocumentService.restoreMerchantDocument(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNotNull();
    }

    @Test
    void deleteMerchantDocumentPermanent_returnsSuccess() {
        pb.merchant_document.MerchantDocumentCommand.ApiResponseMerchantDocumentDelete responseProto = 
                pb.merchant_document.MerchantDocumentCommand.ApiResponseMerchantDocumentDelete.newBuilder()
                .setStatus("success")
                .setMessage("Merchant document permanently deleted")
                .build();

        when(merchantDocumentCommandService.deletePermanent(any(pb.merchant_document.MerchantDocumentOuterClass.FindMerchantDocumentByIdRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        SimpleStatusMessageResponse result = merchantDocumentService.deleteMerchantDocumentPermanent(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("Merchant document permanently deleted");
    }

    @Test
    void restoreAllMerchantDocuments_returnsSuccess() {
        pb.merchant_document.MerchantDocumentCommand.ApiResponseMerchantDocumentAll responseProto = 
                pb.merchant_document.MerchantDocumentCommand.ApiResponseMerchantDocumentAll.newBuilder()
                .setStatus("success")
                .setMessage("All merchant documents restored")
                .build();

        when(merchantDocumentCommandService.restoreAll(any(com.google.protobuf.Empty.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        SimpleStatusMessageResponse result = merchantDocumentService.restoreAllMerchantDocuments().await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("All merchant documents restored");
    }
}