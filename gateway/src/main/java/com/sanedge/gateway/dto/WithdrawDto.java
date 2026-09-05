package com.sanedge.gateway.dto;

import java.util.List;

public class WithdrawDto {

        public record CreateWithdrawBody(
                        String cardNumber,
                        int withdrawAmount) {
        }

        public record UpdateWithdrawBody(
                        String cardNumber,
                        int withdrawAmount) {
        }

        public record WithdrawResponse(
                int withdrawId,
                String withdrawNo,
                String cardNumber,
                int withdrawAmount,
                String withdrawTime,
                String createdAt,
                String updatedAt) {
            public static WithdrawResponse from(pb.withdraw.Withdraw.WithdrawResponse proto) {
                return new WithdrawResponse(
                        proto.getWithdrawId(),
                        proto.getWithdrawNo(),
                        proto.getCardNumber(),
                        proto.getWithdrawAmount(),
                        proto.getWithdrawTime(),
                        proto.getCreatedAt(),
                        proto.getUpdatedAt()
                );
            }
            public static WithdrawResponse from(pb.withdraw.Withdraw.WithdrawResponseDeleteAt proto) {
                return new WithdrawResponse(
                        proto.getWithdrawId(),
                        proto.getWithdrawNo(),
                        proto.getCardNumber(),
                        proto.getWithdrawAmount(),
                        proto.getWithdrawTime(),
                        proto.getCreatedAt(),
                        proto.getUpdatedAt()
                );
            }
        }

        public record FindAllWithdrawResponse(
                List<WithdrawResponse> data,
                String status,
                String message) {
            public static FindAllWithdrawResponse from(pb.withdraw.WithdrawQuery.ApiResponsePaginationWithdraw proto) {
                return new FindAllWithdrawResponse(
                        proto.getDataList().stream().map(WithdrawResponse::from).toList(),
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
            public static FindAllWithdrawResponse from(pb.withdraw.WithdrawQuery.ApiResponsePaginationWithdrawDeleteAt proto) {
                return new FindAllWithdrawResponse(
                        proto.getDataList().stream().map(WithdrawResponse::from).toList(),
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
            public static FindAllWithdrawResponse from(pb.withdraw.Withdraw.ApiResponsesWithdraw proto) {
                return new FindAllWithdrawResponse(
                        proto.getDataList().stream().map(WithdrawResponse::from).toList(),
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
        }

        public record FindByIdWithdrawResponse(
                WithdrawResponse data,
                String status,
                String message) {
            public static FindByIdWithdrawResponse from(pb.withdraw.Withdraw.ApiResponseWithdraw proto) {
                return new FindByIdWithdrawResponse(
                        proto.hasData() ? WithdrawResponse.from(proto.getData()) : null,
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
        }

        public record CreateWithdrawResponse(
                WithdrawResponse data,
                String status,
                String message) {
            public static CreateWithdrawResponse from(pb.withdraw.Withdraw.ApiResponseWithdraw proto) {
                return new CreateWithdrawResponse(
                        proto.hasData() ? WithdrawResponse.from(proto.getData()) : null,
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
        }

        public record UpdateWithdrawResponse(
                WithdrawResponse data,
                String status,
                String message) {
            public static UpdateWithdrawResponse from(pb.withdraw.Withdraw.ApiResponseWithdraw proto) {
                return new UpdateWithdrawResponse(
                        proto.hasData() ? WithdrawResponse.from(proto.getData()) : null,
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
        }

        public record TrashedWithdrawResponse(
                WithdrawResponse data,
                String status,
                String message) {
            public static TrashedWithdrawResponse from(pb.withdraw.Withdraw.ApIResponseWithdrawDeleteAt proto) {
                return new TrashedWithdrawResponse(
                        proto.hasData() ? WithdrawResponse.from(proto.getData()) : null,
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
        }

        @org.eclipse.microprofile.graphql.Name("WithdrawSimpleStatusMessageResponse")
        public record SimpleStatusMessageResponse(
                String status,
                String message) {
            public static SimpleStatusMessageResponse from(pb.withdraw.WithdrawCommand.ApiResponseWithdrawDelete proto) {
                return new SimpleStatusMessageResponse(proto.getStatus(), proto.getMessage());
            }
            public static SimpleStatusMessageResponse from(pb.withdraw.WithdrawCommand.ApiResponseWithdrawAll proto) {
                return new SimpleStatusMessageResponse(proto.getStatus(), proto.getMessage());
            }
        }

        // ── Stats DTOs (from backup, typed per backup pattern) ──

        public record WithdrawMonthlyAmountResponse(
                String month,
                int totalAmount) {
            public static WithdrawMonthlyAmountResponse from(pb.withdraw.stats.WithdrawStatsAmount.WithdrawMonthlyAmountResponse proto) {
                return new WithdrawMonthlyAmountResponse(proto.getMonth(), proto.getTotalAmount());
            }
        }

        public record WithdrawYearlyAmountResponse(
                String year,
                int totalAmount) {
            public static WithdrawYearlyAmountResponse from(pb.withdraw.stats.WithdrawStatsAmount.WithdrawYearlyAmountResponse proto) {
                return new WithdrawYearlyAmountResponse(proto.getYear(), proto.getTotalAmount());
            }
        }

        public record ApiResponseWithdrawMonthAmount(
                String status,
                String message,
                List<WithdrawMonthlyAmountResponse> data) {
            public static ApiResponseWithdrawMonthAmount from(pb.withdraw.stats.WithdrawStatsAmount.ApiResponseWithdrawMonthAmount proto) {
                return new ApiResponseWithdrawMonthAmount(
                        proto.getStatus(),
                        proto.getMessage(),
                        proto.getDataList().stream().map(WithdrawMonthlyAmountResponse::from).toList());
            }
        }

        public record ApiResponseWithdrawYearAmount(
                String status,
                String message,
                List<WithdrawYearlyAmountResponse> data) {
            public static ApiResponseWithdrawYearAmount from(pb.withdraw.stats.WithdrawStatsAmount.ApiResponseWithdrawYearAmount proto) {
                return new ApiResponseWithdrawYearAmount(
                        proto.getStatus(),
                        proto.getMessage(),
                        proto.getDataList().stream().map(WithdrawYearlyAmountResponse::from).toList());
            }
        }

        public record WithdrawMonthStatusSuccessResponse(
                String year,
                String month,
                int totalSuccess,
                int totalAmount) {
            public static WithdrawMonthStatusSuccessResponse from(pb.withdraw.stats.WithdrawStatsStatus.WithdrawMonthStatusSuccessResponse proto) {
                return new WithdrawMonthStatusSuccessResponse(proto.getYear(), proto.getMonth(), proto.getTotalSuccess(), proto.getTotalAmount());
            }
        }

        public record WithdrawYearStatusSuccessResponse(
                String year,
                int totalSuccess,
                int totalAmount) {
            public static WithdrawYearStatusSuccessResponse from(pb.withdraw.stats.WithdrawStatsStatus.WithdrawYearStatusSuccessResponse proto) {
                return new WithdrawYearStatusSuccessResponse(proto.getYear(), proto.getTotalSuccess(), proto.getTotalAmount());
            }
        }

        public record WithdrawMonthStatusFailedResponse(
                String year,
                String month,
                int totalFailed,
                int totalAmount) {
            public static WithdrawMonthStatusFailedResponse from(pb.withdraw.stats.WithdrawStatsStatus.WithdrawMonthStatusFailedResponse proto) {
                return new WithdrawMonthStatusFailedResponse(proto.getYear(), proto.getMonth(), proto.getTotalFailed(), proto.getTotalAmount());
            }
        }

        public record WithdrawYearStatusFailedResponse(
                String year,
                int totalFailed,
                int totalAmount) {
            public static WithdrawYearStatusFailedResponse from(pb.withdraw.stats.WithdrawStatsStatus.WithdrawYearStatusFailedResponse proto) {
                return new WithdrawYearStatusFailedResponse(proto.getYear(), proto.getTotalFailed(), proto.getTotalAmount());
            }
        }

        public record ApiResponseWithdrawMonthStatusSuccess(
                String status,
                String message,
                List<WithdrawMonthStatusSuccessResponse> data) {
            public static ApiResponseWithdrawMonthStatusSuccess from(pb.withdraw.stats.WithdrawStatsStatus.ApiResponseWithdrawMonthStatusSuccess proto) {
                return new ApiResponseWithdrawMonthStatusSuccess(
                        proto.getStatus(),
                        proto.getMessage(),
                        proto.getDataList().stream().map(WithdrawMonthStatusSuccessResponse::from).toList());
            }
        }

        public record ApiResponseWithdrawYearStatusSuccess(
                String status,
                String message,
                List<WithdrawYearStatusSuccessResponse> data) {
            public static ApiResponseWithdrawYearStatusSuccess from(pb.withdraw.stats.WithdrawStatsStatus.ApiResponseWithdrawYearStatusSuccess proto) {
                return new ApiResponseWithdrawYearStatusSuccess(
                        proto.getStatus(),
                        proto.getMessage(),
                        proto.getDataList().stream().map(WithdrawYearStatusSuccessResponse::from).toList());
            }
        }

        public record ApiResponseWithdrawMonthStatusFailed(
                String status,
                String message,
                List<WithdrawMonthStatusFailedResponse> data) {
            public static ApiResponseWithdrawMonthStatusFailed from(pb.withdraw.stats.WithdrawStatsStatus.ApiResponseWithdrawMonthStatusFailed proto) {
                return new ApiResponseWithdrawMonthStatusFailed(
                        proto.getStatus(),
                        proto.getMessage(),
                        proto.getDataList().stream().map(WithdrawMonthStatusFailedResponse::from).toList());
            }
        }

        public record ApiResponseWithdrawYearStatusFailed(
                String status,
                String message,
                List<WithdrawYearStatusFailedResponse> data) {
            public static ApiResponseWithdrawYearStatusFailed from(pb.withdraw.stats.WithdrawStatsStatus.ApiResponseWithdrawYearStatusFailed proto) {
                return new ApiResponseWithdrawYearStatusFailed(
                        proto.getStatus(),
                        proto.getMessage(),
                        proto.getDataList().stream().map(WithdrawYearStatusFailedResponse::from).toList());
            }
        }
}
