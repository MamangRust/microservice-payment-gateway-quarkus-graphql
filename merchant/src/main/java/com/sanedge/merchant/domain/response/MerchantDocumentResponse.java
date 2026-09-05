package com.sanedge.merchant.domain.response;

import com.sanedge.merchant.entity.MerchantDocument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantDocumentResponse {
    private Long documentId;
    private Long merchantId;
    private String documentType;
    private String documentUrl;
    private String status;
    private String note;
    private String createdAt;
    private String updatedAt;

    public static MerchantDocumentResponse from(MerchantDocument doc) {
        if (doc == null) {
            return null;
        }
        return MerchantDocumentResponse.builder()
                .documentId(doc.getDocumentId())
                .merchantId(doc.getMerchantId().longValue())
                .documentType(doc.getDocumentType())
                .documentUrl(doc.getDocumentUrl())
                .status(doc.getStatus())
                .note(doc.getNote())
                .createdAt(doc.getCreatedAt() != null ? doc.getCreatedAt().toString() : null)
                .updatedAt(doc.getUpdatedAt() != null ? doc.getUpdatedAt().toString() : null)
                .build();
    }
}
