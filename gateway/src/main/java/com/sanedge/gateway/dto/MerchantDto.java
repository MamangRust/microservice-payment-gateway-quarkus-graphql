package com.sanedge.gateway.dto;

import java.util.List;

public class MerchantDto {

    public record MerchantResponse(
            int id,
            String name,
            String apiKey,
            String status,
            int userId,
            String createdAt,
            String updatedAt) {
        public static MerchantResponse from(pb.merchant.Merchant.MerchantResponse proto) {
            return new MerchantResponse(
                    proto.getId(),
                    proto.getName(),
                    proto.getApiKey(),
                    proto.getStatus(),
                    proto.getUserId(),
                    proto.getCreatedAt(),
                    proto.getUpdatedAt()
            );
        }
        public static MerchantResponse from(pb.merchant.Merchant.MerchantResponseDeleteAt proto) {
            return new MerchantResponse(
                    proto.getId(),
                    proto.getName(),
                    proto.getApiKey(),
                    proto.getStatus(),
                    proto.getUserId(),
                    proto.getCreatedAt(),
                    proto.getUpdatedAt()
            );
        }
    }

    public record FindAllMerchantResponse(
            List<MerchantResponse> data,
            String status,
            String message) {
        public static FindAllMerchantResponse from(pb.merchant.MerchantQuery.ApiResponsePaginationMerchant proto) {
            return new FindAllMerchantResponse(
                    proto.getDataList().stream().map(MerchantResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
        public static FindAllMerchantResponse from(pb.merchant.MerchantQuery.ApiResponsePaginationMerchantDeleteAt proto) {
            return new FindAllMerchantResponse(
                    proto.getDataList().stream().map(MerchantResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
        public static FindAllMerchantResponse from(pb.merchant.Merchant.ApiResponsesMerchant proto) {
            return new FindAllMerchantResponse(
                    proto.getDataList().stream().map(MerchantResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record FindByIdMerchantResponse(
            MerchantResponse data,
            String status,
            String message) {
        public static FindByIdMerchantResponse from(pb.merchant.Merchant.ApiResponseMerchant proto) {
            return new FindByIdMerchantResponse(
                    proto.hasData() ? MerchantResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record CreateMerchantRequest(
            int userId,
            String name) {}

    public record CreateMerchantResponse(
            MerchantResponse data,
            String status,
            String message) {
        public static CreateMerchantResponse from(pb.merchant.Merchant.ApiResponseMerchant proto) {
            return new CreateMerchantResponse(
                    proto.hasData() ? MerchantResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record UpdateMerchantRequest(
            int userId,
            String name,
            String status) {}

    public record UpdateMerchantResponse(
            MerchantResponse data,
            String status,
            String message) {
        public static UpdateMerchantResponse from(pb.merchant.Merchant.ApiResponseMerchant proto) {
            return new UpdateMerchantResponse(
                    proto.hasData() ? MerchantResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record TrashedMerchantResponse(
            MerchantResponse data,
            String status,
            String message) {
        public static TrashedMerchantResponse from(pb.merchant.Merchant.ApiResponseMerchantDeleteAt proto) {
            return new TrashedMerchantResponse(
                    proto.hasData() ? MerchantResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    @org.eclipse.microprofile.graphql.Name("MerchantSimpleStatusMessageResponse")
    public record SimpleStatusMessageResponse(
            String status,
            String message) {
        public static SimpleStatusMessageResponse from(pb.merchant.MerchantCommand.ApiResponseMerchantDelete proto) {
            return new SimpleStatusMessageResponse(proto.getStatus(), proto.getMessage());
        }
        public static SimpleStatusMessageResponse from(pb.merchant.MerchantCommand.ApiResponseMerchantAll proto) {
            return new SimpleStatusMessageResponse(proto.getStatus(), proto.getMessage());
        }
    }

    // ── Stats DTOs (from backup, typed per backup pattern) ──

    public record MerchantResponseMonthlyAmount(
            String month,
            long totalAmount) {
        public static MerchantResponseMonthlyAmount from(
                pb.merchant.stats.MerchantStatsAmount.MerchantResponseMonthlyAmount proto) {
            return new MerchantResponseMonthlyAmount(proto.getMonth(), proto.getTotalAmount());
        }
    }

    public record MerchantResponseYearlyAmount(
            String year,
            long totalAmount) {
        public static MerchantResponseYearlyAmount from(
                pb.merchant.stats.MerchantStatsAmount.MerchantResponseYearlyAmount proto) {
            return new MerchantResponseYearlyAmount(proto.getYear(), proto.getTotalAmount());
        }
    }

    public record ApiResponseMerchantMonthlyAmount(
            String status,
            String message,
            List<MerchantResponseMonthlyAmount> data) {
        public static ApiResponseMerchantMonthlyAmount from(
                pb.merchant.stats.MerchantStatsAmount.ApiResponseMerchantMonthlyAmount proto) {
            return new ApiResponseMerchantMonthlyAmount(
                    proto.getStatus(),
                    proto.getMessage(),
                    proto.getDataList().stream().map(MerchantResponseMonthlyAmount::from).toList());
        }
    }

    public record ApiResponseMerchantYearlyAmount(
            String status,
            String message,
            List<MerchantResponseYearlyAmount> data) {
        public static ApiResponseMerchantYearlyAmount from(
                pb.merchant.stats.MerchantStatsAmount.ApiResponseMerchantYearlyAmount proto) {
            return new ApiResponseMerchantYearlyAmount(
                    proto.getStatus(),
                    proto.getMessage(),
                    proto.getDataList().stream().map(MerchantResponseYearlyAmount::from).toList());
        }
    }

    public record MerchantResponseMonthlyPaymentMethod(
            String month,
            String paymentMethod,
            long totalAmount) {
        public static MerchantResponseMonthlyPaymentMethod from(
                pb.merchant.stats.MerchantStatsMethod.MerchantResponseMonthlyPaymentMethod proto) {
            return new MerchantResponseMonthlyPaymentMethod(proto.getMonth(), proto.getPaymentMethod(),
                    proto.getTotalAmount());
        }
    }

    public record MerchantResponseYearlyPaymentMethod(
            String year,
            String paymentMethod,
            long totalAmount) {
        public static MerchantResponseYearlyPaymentMethod from(
                pb.merchant.stats.MerchantStatsMethod.MerchantResponseYearlyPaymentMethod proto) {
            return new MerchantResponseYearlyPaymentMethod(proto.getYear(), proto.getPaymentMethod(),
                    proto.getTotalAmount());
        }
    }

    public record ApiResponseMerchantMonthlyPaymentMethod(
            String status,
            String message,
            List<MerchantResponseMonthlyPaymentMethod> data) {
        public static ApiResponseMerchantMonthlyPaymentMethod from(
                pb.merchant.stats.MerchantStatsMethod.ApiResponseMerchantMonthlyPaymentMethod proto) {
            return new ApiResponseMerchantMonthlyPaymentMethod(
                    proto.getStatus(),
                    proto.getMessage(),
                    proto.getDataList().stream().map(MerchantResponseMonthlyPaymentMethod::from).toList());
        }
    }

    public record ApiResponseMerchantYearlyPaymentMethod(
            String status,
            String message,
            List<MerchantResponseYearlyPaymentMethod> data) {
        public static ApiResponseMerchantYearlyPaymentMethod from(
                pb.merchant.stats.MerchantStatsMethod.ApiResponseMerchantYearlyPaymentMethod proto) {
            return new ApiResponseMerchantYearlyPaymentMethod(
                    proto.getStatus(),
                    proto.getMessage(),
                    proto.getDataList().stream().map(MerchantResponseYearlyPaymentMethod::from).toList());
        }
    }

    public record MerchantResponseMonthlyTotalAmount(
            String month,
            String year,
            long totalAmount) {
        public static MerchantResponseMonthlyTotalAmount from(
                pb.merchant.stats.MerchantStatsTotalamount.MerchantResponseMonthlyTotalAmount proto) {
            return new MerchantResponseMonthlyTotalAmount(proto.getMonth(), proto.getYear(), proto.getTotalAmount());
        }
    }

    public record MerchantResponseYearlyTotalAmount(
            String year,
            long totalAmount) {
        public static MerchantResponseYearlyTotalAmount from(
                pb.merchant.stats.MerchantStatsTotalamount.MerchantResponseYearlyTotalAmount proto) {
            return new MerchantResponseYearlyTotalAmount(proto.getYear(), proto.getTotalAmount());
        }
    }

    public record ApiResponseMerchantMonthlyTotalAmount(
            String status,
            String message,
            List<MerchantResponseMonthlyTotalAmount> data) {
        public static ApiResponseMerchantMonthlyTotalAmount from(
                pb.merchant.stats.MerchantStatsTotalamount.ApiResponseMerchantMonthlyTotalAmount proto) {
            return new ApiResponseMerchantMonthlyTotalAmount(
                    proto.getStatus(),
                    proto.getMessage(),
                    proto.getDataList().stream().map(MerchantResponseMonthlyTotalAmount::from).toList());
        }
    }

    public record ApiResponseMerchantYearlyTotalAmount(
            String status,
            String message,
            List<MerchantResponseYearlyTotalAmount> data) {
        public static ApiResponseMerchantYearlyTotalAmount from(
                pb.merchant.stats.MerchantStatsTotalamount.ApiResponseMerchantYearlyTotalAmount proto) {
            return new ApiResponseMerchantYearlyTotalAmount(
                    proto.getStatus(),
                    proto.getMessage(),
                    proto.getDataList().stream().map(MerchantResponseYearlyTotalAmount::from).toList());
        }
    }

    public record PaginationMeta(
            int currentPage,
            int pageSize,
            int totalPage,
            int totalRecords) {
        public static PaginationMeta from(pb.common.PaginationMeta proto) {
            return new PaginationMeta(
                    proto.getCurrentPage(),
                    proto.getPageSize(),
                    proto.getTotalPages(),
                    proto.getTotalRecords());
        }
    }

    public record MerchantTransactionResponse(
            int id,
            String cardNumber,
            int amount,
            String paymentMethod,
            int merchantId,
            String merchantName,
            String transactionTime,
            String createdAt,
            String updatedAt,
            String deletedAt) {
        public static MerchantTransactionResponse from(
                pb.merchant.MerchantTransaction.MerchantTransactionResponse proto) {
            return new MerchantTransactionResponse(
                    proto.getId(),
                    proto.getCardNumber(),
                    proto.getAmount(),
                    proto.getPaymentMethod(),
                    proto.getMerchantId(),
                    proto.getMerchantName(),
                    proto.getTransactionTime(),
                    proto.getCreatedAt(),
                    proto.getUpdatedAt(),
                    proto.hasDeletedAt() ? proto.getDeletedAt().getValue() : null);
        }
    }

    public record ApiResponsePaginationMerchantTransaction(
            String status,
            String message,
            List<MerchantTransactionResponse> data,
            PaginationMeta paginationMeta) {
        public static ApiResponsePaginationMerchantTransaction from(
                pb.merchant.MerchantTransaction.ApiResponsePaginationMerchantTransaction proto) {
            return new ApiResponsePaginationMerchantTransaction(
                    proto.getStatus(),
                    proto.getMessage(),
                    proto.getDataList().stream().map(MerchantTransactionResponse::from).toList(),
                    proto.hasPaginationMeta() ? PaginationMeta.from(proto.getPaginationMeta()) : null);
        }
    }
}
