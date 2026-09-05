package com.sanedge.gateway.dto;

import java.util.List;

public class TransferDto {

        public record CreateTransferRequest(
                String transferFrom,
                String transferTo,
                int transferAmount) {}

        public record UpdateTransferRequest(
                String transferFrom,
                String transferTo,
                int transferAmount) {}

        public record TransferResponse(
                int id,
                String transferNo,
                String transferFrom,
                String transferTo,
                int transferAmount,
                String transferTime,
                String createdAt,
                String updatedAt) {
            public static TransferResponse from(pb.transfer.Transfer.TransferResponse proto) {
                return new TransferResponse(
                        proto.getId(),
                        proto.getTransferNo(),
                        proto.getTransferFrom(),
                        proto.getTransferTo(),
                        proto.getTransferAmount(),
                        proto.getTransferTime(),
                        proto.getCreatedAt(),
                        proto.getUpdatedAt()
                );
            }
            public static TransferResponse from(pb.transfer.Transfer.TransferResponseDeleteAt proto) {
                return new TransferResponse(
                        proto.getId(),
                        proto.getTransferNo(),
                        proto.getTransferFrom(),
                        proto.getTransferTo(),
                        proto.getTransferAmount(),
                        proto.getTransferTime(),
                        proto.getCreatedAt(),
                        proto.getUpdatedAt()
                );
            }
        }

        public record FindAllTransferResponse(
                List<TransferResponse> data,
                String status,
                String message) {
            public static FindAllTransferResponse from(pb.transfer.TransferQuery.ApiResponsePaginationTransfer proto) {
                return new FindAllTransferResponse(
                        proto.getDataList().stream().map(TransferResponse::from).toList(),
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
            public static FindAllTransferResponse from(pb.transfer.TransferQuery.ApiResponsePaginationTransferDeleteAt proto) {
                return new FindAllTransferResponse(
                        proto.getDataList().stream().map(TransferResponse::from).toList(),
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
            public static FindAllTransferResponse from(pb.transfer.TransferQuery.ApiResponseTransfers proto) {
                return new FindAllTransferResponse(
                        proto.getDataList().stream().map(TransferResponse::from).toList(),
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
        }

        public record FindByIdTransferResponse(
                TransferResponse data,
                String status,
                String message) {
            public static FindByIdTransferResponse from(pb.transfer.Transfer.ApiResponseTransfer proto) {
                return new FindByIdTransferResponse(
                        proto.hasData() ? TransferResponse.from(proto.getData()) : null,
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
        }

        public record CreateTransferResponse(
                TransferResponse data,
                String status,
                String message) {
            public static CreateTransferResponse from(pb.transfer.Transfer.ApiResponseTransfer proto) {
                return new CreateTransferResponse(
                        proto.hasData() ? TransferResponse.from(proto.getData()) : null,
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
        }

        public record UpdateTransferResponse(
                TransferResponse data,
                String status,
                String message) {
            public static UpdateTransferResponse from(pb.transfer.Transfer.ApiResponseTransfer proto) {
                return new UpdateTransferResponse(
                        proto.hasData() ? TransferResponse.from(proto.getData()) : null,
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
        }

        public record TrashedTransferResponse(
                TransferResponse data,
                String status,
                String message) {
            public static TrashedTransferResponse from(pb.transfer.Transfer.ApIResponseTransferDeleteAt proto) {
                return new TrashedTransferResponse(
                        proto.hasData() ? TransferResponse.from(proto.getData()) : null,
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
        }

        @org.eclipse.microprofile.graphql.Name("TransferSimpleStatusMessageResponse")
        public record SimpleStatusMessageResponse(
                String status,
                String message) {
            public static SimpleStatusMessageResponse from(pb.transfer.TransferCommand.ApiResponseTransferDelete proto) {
                return new SimpleStatusMessageResponse(proto.getStatus(), proto.getMessage());
            }
            public static SimpleStatusMessageResponse from(pb.transfer.TransferCommand.ApiResponseTransferAll proto) {
                return new SimpleStatusMessageResponse(proto.getStatus(), proto.getMessage());
            }
        }

        // ── Stats DTOs (from backup, typed per backup pattern) ──

        public record TransferMonthAmountResponse(
                String month,
                int totalAmount) {
            public static TransferMonthAmountResponse from(pb.transfer.stats.TransferStatsAmount.TransferMonthAmountResponse proto) {
                return new TransferMonthAmountResponse(proto.getMonth(), proto.getTotalAmount());
            }
        }

        public record TransferYearAmountResponse(
                String year,
                int totalAmount) {
            public static TransferYearAmountResponse from(pb.transfer.stats.TransferStatsAmount.TransferYearAmountResponse proto) {
                return new TransferYearAmountResponse(proto.getYear(), proto.getTotalAmount());
            }
        }

        public record ApiResponseTransferMonthAmount(
                String status,
                String message,
                List<TransferMonthAmountResponse> data) {
            public static ApiResponseTransferMonthAmount from(pb.transfer.stats.TransferStatsAmount.ApiResponseTransferMonthAmount proto) {
                return new ApiResponseTransferMonthAmount(
                        proto.getStatus(),
                        proto.getMessage(),
                        proto.getDataList().stream().map(TransferMonthAmountResponse::from).toList());
            }
        }

        public record ApiResponseTransferYearAmount(
                String status,
                String message,
                List<TransferYearAmountResponse> data) {
            public static ApiResponseTransferYearAmount from(pb.transfer.stats.TransferStatsAmount.ApiResponseTransferYearAmount proto) {
                return new ApiResponseTransferYearAmount(
                        proto.getStatus(),
                        proto.getMessage(),
                        proto.getDataList().stream().map(TransferYearAmountResponse::from).toList());
            }
        }

        public record TransferMonthStatusSuccessResponse(
                String year,
                String month,
                int totalSuccess,
                int totalAmount) {
            public static TransferMonthStatusSuccessResponse from(pb.transfer.stats.TransferStatsStatus.TransferMonthStatusSuccessResponse proto) {
                return new TransferMonthStatusSuccessResponse(proto.getYear(), proto.getMonth(), proto.getTotalSuccess(), proto.getTotalAmount());
            }
        }

        public record TransferYearStatusSuccessResponse(
                String year,
                int totalSuccess,
                int totalAmount) {
            public static TransferYearStatusSuccessResponse from(pb.transfer.stats.TransferStatsStatus.TransferYearStatusSuccessResponse proto) {
                return new TransferYearStatusSuccessResponse(proto.getYear(), proto.getTotalSuccess(), proto.getTotalAmount());
            }
        }

        public record TransferMonthStatusFailedResponse(
                String year,
                String month,
                int totalFailed,
                int totalAmount) {
            public static TransferMonthStatusFailedResponse from(pb.transfer.stats.TransferStatsStatus.TransferMonthStatusFailedResponse proto) {
                return new TransferMonthStatusFailedResponse(proto.getYear(), proto.getMonth(), proto.getTotalFailed(), proto.getTotalAmount());
            }
        }

        public record TransferYearStatusFailedResponse(
                String year,
                int totalFailed,
                int totalAmount) {
            public static TransferYearStatusFailedResponse from(pb.transfer.stats.TransferStatsStatus.TransferYearStatusFailedResponse proto) {
                return new TransferYearStatusFailedResponse(proto.getYear(), proto.getTotalFailed(), proto.getTotalAmount());
            }
        }

        public record ApiResponseTransferMonthStatusSuccess(
                String status,
                String message,
                List<TransferMonthStatusSuccessResponse> data) {
            public static ApiResponseTransferMonthStatusSuccess from(pb.transfer.stats.TransferStatsStatus.ApiResponseTransferMonthStatusSuccess proto) {
                return new ApiResponseTransferMonthStatusSuccess(
                        proto.getStatus(),
                        proto.getMessage(),
                        proto.getDataList().stream().map(TransferMonthStatusSuccessResponse::from).toList());
            }
        }

        public record ApiResponseTransferYearStatusSuccess(
                String status,
                String message,
                List<TransferYearStatusSuccessResponse> data) {
            public static ApiResponseTransferYearStatusSuccess from(pb.transfer.stats.TransferStatsStatus.ApiResponseTransferYearStatusSuccess proto) {
                return new ApiResponseTransferYearStatusSuccess(
                        proto.getStatus(),
                        proto.getMessage(),
                        proto.getDataList().stream().map(TransferYearStatusSuccessResponse::from).toList());
            }
        }

        public record ApiResponseTransferMonthStatusFailed(
                String status,
                String message,
                List<TransferMonthStatusFailedResponse> data) {
            public static ApiResponseTransferMonthStatusFailed from(pb.transfer.stats.TransferStatsStatus.ApiResponseTransferMonthStatusFailed proto) {
                return new ApiResponseTransferMonthStatusFailed(
                        proto.getStatus(),
                        proto.getMessage(),
                        proto.getDataList().stream().map(TransferMonthStatusFailedResponse::from).toList());
            }
        }

        public record ApiResponseTransferYearStatusFailed(
                String status,
                String message,
                List<TransferYearStatusFailedResponse> data) {
            public static ApiResponseTransferYearStatusFailed from(pb.transfer.stats.TransferStatsStatus.ApiResponseTransferYearStatusFailed proto) {
                return new ApiResponseTransferYearStatusFailed(
                        proto.getStatus(),
                        proto.getMessage(),
                        proto.getDataList().stream().map(TransferYearStatusFailedResponse::from).toList());
            }
        }
}
