package com.sanedge.gateway.dto;

import java.util.List;

public class MerchantDocumentDto {

    public record CreateMerchantDocumentBody(
            int merchantId,
            String documentType,
            String documentUrl
    ) {}

    public record UpdateMerchantDocumentBody(
            int merchantId,
            String documentType,
            String documentUrl,
            String note,
            String status
    ) {}

    public record UpdateMerchantDocumentStatusBody(
            int merchantId,
            String note,
            String status
    ) {}

    public record MerchantDocumentResponse(
            int documentId,
            int merchantId,
            String documentType,
            String documentUrl,
            String status,
            String note,
            String uploadedAt,
            String updatedAt) {
        public static MerchantDocumentResponse from(pb.merchant_document.MerchantDocumentOuterClass.MerchantDocument proto) {
            return new MerchantDocumentResponse(
                    proto.getDocumentId(),
                    proto.getMerchantId(),
                    proto.getDocumentType(),
                    proto.getDocumentUrl(),
                    proto.getStatus(),
                    proto.getNote(),
                    proto.getUploadedAt(),
                    proto.getUpdatedAt()
            );
        }
        public static MerchantDocumentResponse from(pb.merchant_document.MerchantDocumentOuterClass.MerchantDocumentDeleteAt proto) {
            return new MerchantDocumentResponse(
                    proto.getDocumentId(),
                    proto.getMerchantId(),
                    proto.getDocumentType(),
                    proto.getDocumentUrl(),
                    proto.getStatus(),
                    proto.getNote(),
                    proto.getUploadedAt(),
                    proto.getUpdatedAt()
            );
        }
    }

    public record FindAllMerchantDocumentsResponse(
            List<MerchantDocumentResponse> data,
            String status,
            String message) {
        public static FindAllMerchantDocumentsResponse from(pb.merchant_document.MerchantDocumentQuery.ApiResponsePaginationMerchantDocument proto) {
            return new FindAllMerchantDocumentsResponse(
                    proto.getDataList().stream().map(MerchantDocumentResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
        public static FindAllMerchantDocumentsResponse from(pb.merchant_document.MerchantDocumentQuery.ApiResponsePaginationMerchantDocumentAt proto) {
            return new FindAllMerchantDocumentsResponse(
                    proto.getDataList().stream().map(MerchantDocumentResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record FindByIdMerchantDocumentResponse(
            MerchantDocumentResponse data,
            String status,
            String message) {
        public static FindByIdMerchantDocumentResponse from(pb.merchant_document.MerchantDocumentOuterClass.ApiResponseMerchantDocument proto) {
            return new FindByIdMerchantDocumentResponse(
                    proto.hasData() ? MerchantDocumentResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record CreateMerchantDocumentResponse(
            MerchantDocumentResponse data,
            String status,
            String message) {
        public static CreateMerchantDocumentResponse from(pb.merchant_document.MerchantDocumentOuterClass.ApiResponseMerchantDocument proto) {
            return new CreateMerchantDocumentResponse(
                    proto.hasData() ? MerchantDocumentResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record UpdateMerchantDocumentResponse(
            MerchantDocumentResponse data,
            String status,
            String message) {
        public static UpdateMerchantDocumentResponse from(pb.merchant_document.MerchantDocumentOuterClass.ApiResponseMerchantDocument proto) {
            return new UpdateMerchantDocumentResponse(
                    proto.hasData() ? MerchantDocumentResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record TrashedMerchantDocumentResponse(
            MerchantDocumentResponse data,
            String status,
            String message) {
        public static TrashedMerchantDocumentResponse from(pb.merchant_document.MerchantDocumentOuterClass.ApiResponseMerchantDocumentDeleteAt proto) {
            return new TrashedMerchantDocumentResponse(
                    proto.hasData() ? MerchantDocumentResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
                );
        }
    }

    @org.eclipse.microprofile.graphql.Name("MerchantDocumentSimpleStatusMessageResponse")
    public record SimpleStatusMessageResponse(
            String status,
            String message) {
        public static SimpleStatusMessageResponse from(pb.merchant_document.MerchantDocumentCommand.ApiResponseMerchantDocumentDelete proto) {
            return new SimpleStatusMessageResponse(proto.getStatus(), proto.getMessage());
        }
        public static SimpleStatusMessageResponse from(pb.merchant_document.MerchantDocumentCommand.ApiResponseMerchantDocumentAll proto) {
            return new SimpleStatusMessageResponse(proto.getStatus(), proto.getMessage());
        }
    }
}
