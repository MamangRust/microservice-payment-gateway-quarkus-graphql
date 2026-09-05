package com.sanedge.gateway.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.gateway.dto.MerchantDocumentDto;
import com.sanedge.gateway.service.MerchantDocumentService;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class MerchantDocumentResourceTest {

    @Mock private MerchantDocumentService merchantDocumentService;
    private MerchantDocumentResource merchantDocumentResource;

    @BeforeEach
    void setUp() throws Exception {
        merchantDocumentResource = new MerchantDocumentResource();
        Field f = MerchantDocumentResource.class.getDeclaredField("merchantDocumentService");
        f.setAccessible(true);
        f.set(merchantDocumentResource, merchantDocumentService);
    }

    @Test
    void listMerchantDocuments_Success() {
        MerchantDocumentDto.FindAllMerchantDocumentsResponse dto = new MerchantDocumentDto.FindAllMerchantDocumentsResponse(List.of(), "success", "ok");
        lenient().when(merchantDocumentService.listMerchantDocuments(anyInt(), anyInt(), anyString())).thenReturn(Uni.createFrom().item(dto));
        MerchantDocumentDto.FindAllMerchantDocumentsResponse result = merchantDocumentResource.listMerchantDocuments(1, 10, "").await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void getMerchantDocument_Success() {
        MerchantDocumentDto.FindByIdMerchantDocumentResponse dto = new MerchantDocumentDto.FindByIdMerchantDocumentResponse(null, "success", "ok");
        lenient().when(merchantDocumentService.getMerchantDocument(anyInt())).thenReturn(Uni.createFrom().item(dto));
        MerchantDocumentDto.FindByIdMerchantDocumentResponse result = merchantDocumentResource.getMerchantDocument(1).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void createMerchantDocument_Success() {
        MerchantDocumentDto.CreateMerchantDocumentResponse dto = new MerchantDocumentDto.CreateMerchantDocumentResponse(null, "success", "created");
        lenient().when(merchantDocumentService.createMerchantDocument(any())).thenReturn(Uni.createFrom().item(dto));
        MerchantDocumentDto.CreateMerchantDocumentResponse result = merchantDocumentResource.createMerchantDocument(new MerchantDocumentDto.CreateMerchantDocumentBody(1, "PDF", "http://doc.url")).await().indefinitely();
        assertThat(result.message()).isEqualTo("created");
    }

    @Test
    void restoreAllMerchantDocuments_Success() {
        MerchantDocumentDto.SimpleStatusMessageResponse dto = new MerchantDocumentDto.SimpleStatusMessageResponse("success", "ok");
        lenient().when(merchantDocumentService.restoreAllMerchantDocuments()).thenReturn(Uni.createFrom().item(dto));
        MerchantDocumentDto.SimpleStatusMessageResponse result = merchantDocumentResource.restoreAllMerchantDocuments().await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }
}
