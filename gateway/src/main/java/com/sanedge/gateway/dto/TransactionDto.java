package com.sanedge.gateway.dto;

import java.util.List;

public class TransactionDto {

        public record CreateTransactionBody(
                        String cardNumber,
                        int amount,
                        String paymentMethod,
                        int merchantId) {
        }

        public record UpdateTransactionBody(
                        String cardNumber,
                        int amount,
                        String paymentMethod,
                        int merchantId) {
        }

        public record TransactionResponse(
                int id,
                String cardNumber,
                String transactionNo,
                int amount,
                String paymentMethod,
                int merchantId,
                String transactionTime,
                String createdAt,
                String updatedAt) {
            public static TransactionResponse from(pb.transaction.Transaction.TransactionResponse proto) {
                return new TransactionResponse(
                        proto.getId(),
                        proto.getCardNumber(),
                        proto.getTransactionNo(),
                        proto.getAmount(),
                        proto.getPaymentMethod(),
                        proto.getMerchantId(),
                        proto.getTransactionTime(),
                        proto.getCreatedAt(),
                        proto.getUpdatedAt()
                );
            }
            public static TransactionResponse from(pb.transaction.Transaction.TransactionResponseDeleteAt proto) {
                return new TransactionResponse(
                        proto.getId(),
                        proto.getCardNumber(),
                        proto.getTransactionNo(),
                        proto.getAmount(),
                        proto.getPaymentMethod(),
                        proto.getMerchantId(),
                        proto.getTransactionTime(),
                        proto.getCreatedAt(),
                        proto.getUpdatedAt()
                );
            }
        }

        public record FindAllTransactionResponse(
                List<TransactionResponse> data,
                String status,
                String message) {
            public static FindAllTransactionResponse from(pb.transaction.TransactionQuery.ApiResponsePaginationTransaction proto) {
                return new FindAllTransactionResponse(
                        proto.getDataList().stream().map(TransactionResponse::from).toList(),
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
            public static FindAllTransactionResponse from(pb.transaction.TransactionQuery.ApiResponsePaginationTransactionDeleteAt proto) {
                return new FindAllTransactionResponse(
                        proto.getDataList().stream().map(TransactionResponse::from).toList(),
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
            public static FindAllTransactionResponse from(pb.transaction.Transaction.ApiResponseTransactions proto) {
                return new FindAllTransactionResponse(
                        proto.getDataList().stream().map(TransactionResponse::from).toList(),
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
        }

        public record FindByIdTransactionResponse(
                TransactionResponse data,
                String status,
                String message) {
            public static FindByIdTransactionResponse from(pb.transaction.Transaction.ApiResponseTransaction proto) {
                return new FindByIdTransactionResponse(
                        proto.hasData() ? TransactionResponse.from(proto.getData()) : null,
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
        }

        public record CreateTransactionResponse(
                TransactionResponse data,
                String status,
                String message) {
            public static CreateTransactionResponse from(pb.transaction.Transaction.ApiResponseTransaction proto) {
                return new CreateTransactionResponse(
                        proto.hasData() ? TransactionResponse.from(proto.getData()) : null,
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
        }

        public record UpdateTransactionResponse(
                TransactionResponse data,
                String status,
                String message) {
            public static UpdateTransactionResponse from(pb.transaction.Transaction.ApiResponseTransaction proto) {
                return new UpdateTransactionResponse(
                        proto.hasData() ? TransactionResponse.from(proto.getData()) : null,
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
        }

        public record TrashedTransactionResponse(
                TransactionResponse data,
                String status,
                String message) {
            public static TrashedTransactionResponse from(pb.transaction.Transaction.ApiResponseTransactionDeleteAt proto) {
                return new TrashedTransactionResponse(
                        proto.hasData() ? TransactionResponse.from(proto.getData()) : null,
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
        }

        @org.eclipse.microprofile.graphql.Name("TransactionSimpleStatusMessageResponse")
        public record SimpleStatusMessageResponse(
                String status,
                String message) {
            public static SimpleStatusMessageResponse from(pb.transaction.TransactionCommand.ApiResponseTransactionDelete proto) {
                return new SimpleStatusMessageResponse(proto.getStatus(), proto.getMessage());
            }
            public static SimpleStatusMessageResponse from(pb.transaction.TransactionCommand.ApiResponseTransactionAll proto) {
                return new SimpleStatusMessageResponse(proto.getStatus(), proto.getMessage());
            }
        }

        // ── Stats DTOs (from backup, typed per backup pattern) ──

        public record TransactionMonthAmountResponse(
                String month,
                int totalAmount) {
            public static TransactionMonthAmountResponse from(pb.transaction.stats.TransactionStatsAmount.TransactionMonthAmountResponse proto) {
                return new TransactionMonthAmountResponse(proto.getMonth(), proto.getTotalAmount());
            }
        }

        public record TransactionYearlyAmountResponse(
                String year,
                int totalAmount) {
            public static TransactionYearlyAmountResponse from(pb.transaction.stats.TransactionStatsAmount.TransactionYearlyAmountResponse proto) {
                return new TransactionYearlyAmountResponse(proto.getYear(), proto.getTotalAmount());
            }
        }

        public record ApiResponseTransactionMonthAmount(
                String status,
                String message,
                List<TransactionMonthAmountResponse> data) {
            public static ApiResponseTransactionMonthAmount from(pb.transaction.stats.TransactionStatsAmount.ApiResponseTransactionMonthAmount proto) {
                return new ApiResponseTransactionMonthAmount(
                        proto.getStatus(),
                        proto.getMessage(),
                        proto.getDataList().stream().map(TransactionMonthAmountResponse::from).toList());
            }
        }

        public record ApiResponseTransactionYearAmount(
                String status,
                String message,
                List<TransactionYearlyAmountResponse> data) {
            public static ApiResponseTransactionYearAmount from(pb.transaction.stats.TransactionStatsAmount.ApiResponseTransactionYearAmount proto) {
                return new ApiResponseTransactionYearAmount(
                        proto.getStatus(),
                        proto.getMessage(),
                        proto.getDataList().stream().map(TransactionYearlyAmountResponse::from).toList());
            }
        }

        public record TransactionMonthMethodResponse(
                String month,
                String paymentMethod,
                int totalTransactions,
                int totalAmount) {
            public static TransactionMonthMethodResponse from(pb.transaction.stats.TransactionStatsMethod.TransactionMonthMethodResponse proto) {
                return new TransactionMonthMethodResponse(proto.getMonth(), proto.getPaymentMethod(), proto.getTotalTransactions(), proto.getTotalAmount());
            }
        }

        public record TransactionYearMethodResponse(
                String year,
                String paymentMethod,
                int totalTransactions,
                int totalAmount) {
            public static TransactionYearMethodResponse from(pb.transaction.stats.TransactionStatsMethod.TransactionYearMethodResponse proto) {
                return new TransactionYearMethodResponse(proto.getYear(), proto.getPaymentMethod(), proto.getTotalTransactions(), proto.getTotalAmount());
            }
        }

        public record ApiResponseTransactionMonthMethod(
                String status,
                String message,
                List<TransactionMonthMethodResponse> data) {
            public static ApiResponseTransactionMonthMethod from(pb.transaction.stats.TransactionStatsMethod.ApiResponseTransactionMonthMethod proto) {
                return new ApiResponseTransactionMonthMethod(
                        proto.getStatus(),
                        proto.getMessage(),
                        proto.getDataList().stream().map(TransactionMonthMethodResponse::from).toList());
            }
        }

        public record ApiResponseTransactionYearMethod(
                String status,
                String message,
                List<TransactionYearMethodResponse> data) {
            public static ApiResponseTransactionYearMethod from(pb.transaction.stats.TransactionStatsMethod.ApiResponseTransactionYearMethod proto) {
                return new ApiResponseTransactionYearMethod(
                        proto.getStatus(),
                        proto.getMessage(),
                        proto.getDataList().stream().map(TransactionYearMethodResponse::from).toList());
            }
        }

        public record TransactionMonthStatusSuccessResponse(
                String year,
                String month,
                int totalSuccess,
                int totalAmount) {
            public static TransactionMonthStatusSuccessResponse from(pb.transaction.stats.TransactionStatsStatus.TransactionMonthStatusSuccessResponse proto) {
                return new TransactionMonthStatusSuccessResponse(proto.getYear(), proto.getMonth(), proto.getTotalSuccess(), proto.getTotalAmount());
            }
        }

        public record TransactionYearStatusSuccessResponse(
                String year,
                int totalSuccess,
                int totalAmount) {
            public static TransactionYearStatusSuccessResponse from(pb.transaction.stats.TransactionStatsStatus.TransactionYearStatusSuccessResponse proto) {
                return new TransactionYearStatusSuccessResponse(proto.getYear(), proto.getTotalSuccess(), proto.getTotalAmount());
            }
        }

        public record TransactionMonthStatusFailedResponse(
                String year,
                String month,
                int totalFailed,
                int totalAmount) {
            public static TransactionMonthStatusFailedResponse from(pb.transaction.stats.TransactionStatsStatus.TransactionMonthStatusFailedResponse proto) {
                return new TransactionMonthStatusFailedResponse(proto.getYear(), proto.getMonth(), proto.getTotalFailed(), proto.getTotalAmount());
            }
        }

        public record TransactionYearStatusFailedResponse(
                String year,
                int totalFailed,
                int totalAmount) {
            public static TransactionYearStatusFailedResponse from(pb.transaction.stats.TransactionStatsStatus.TransactionYearStatusFailedResponse proto) {
                return new TransactionYearStatusFailedResponse(proto.getYear(), proto.getTotalFailed(), proto.getTotalAmount());
            }
        }

        public record ApiResponseTransactionMonthStatusSuccess(
                String status,
                String message,
                List<TransactionMonthStatusSuccessResponse> data) {
            public static ApiResponseTransactionMonthStatusSuccess from(pb.transaction.stats.TransactionStatsStatus.ApiResponseTransactionMonthStatusSuccess proto) {
                return new ApiResponseTransactionMonthStatusSuccess(
                        proto.getStatus(),
                        proto.getMessage(),
                        proto.getDataList().stream().map(TransactionMonthStatusSuccessResponse::from).toList());
            }
        }

        public record ApiResponseTransactionYearStatusSuccess(
                String status,
                String message,
                List<TransactionYearStatusSuccessResponse> data) {
            public static ApiResponseTransactionYearStatusSuccess from(pb.transaction.stats.TransactionStatsStatus.ApiResponseTransactionYearStatusSuccess proto) {
                return new ApiResponseTransactionYearStatusSuccess(
                        proto.getStatus(),
                        proto.getMessage(),
                        proto.getDataList().stream().map(TransactionYearStatusSuccessResponse::from).toList());
            }
        }

        public record ApiResponseTransactionMonthStatusFailed(
                String status,
                String message,
                List<TransactionMonthStatusFailedResponse> data) {
            public static ApiResponseTransactionMonthStatusFailed from(pb.transaction.stats.TransactionStatsStatus.ApiResponseTransactionMonthStatusFailed proto) {
                return new ApiResponseTransactionMonthStatusFailed(
                        proto.getStatus(),
                        proto.getMessage(),
                        proto.getDataList().stream().map(TransactionMonthStatusFailedResponse::from).toList());
            }
        }

        public record ApiResponseTransactionYearStatusFailed(
                String status,
                String message,
                List<TransactionYearStatusFailedResponse> data) {
            public static ApiResponseTransactionYearStatusFailed from(pb.transaction.stats.TransactionStatsStatus.ApiResponseTransactionYearStatusFailed proto) {
                return new ApiResponseTransactionYearStatusFailed(
                        proto.getStatus(),
                        proto.getMessage(),
                        proto.getDataList().stream().map(TransactionYearStatusFailedResponse::from).toList());
            }
        }
}
