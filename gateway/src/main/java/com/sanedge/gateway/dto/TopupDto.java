package com.sanedge.gateway.dto;

import java.util.List;

public class TopupDto {

        public record CreateTopupRequest(
                String cardNumber,
                int topupAmount,
                String topupMethod) {}

        public record UpdateTopupRequest(
                String cardNumber,
                int topupAmount,
                String topupMethod) {}

        public record TopupResponse(
                int id,
                String cardNumber,
                String topupNo,
                int topupAmount,
                String topupMethod,
                String topupTime,
                String createdAt,
                String updatedAt) {
            public static TopupResponse from(pb.topup.Topup.TopupResponse proto) {
                return new TopupResponse(
                        proto.getId(),
                        proto.getCardNumber(),
                        proto.getTopupNo(),
                        proto.getTopupAmount(),
                        proto.getTopupMethod(),
                        proto.getTopupTime(),
                        proto.getCreatedAt(),
                        proto.getUpdatedAt()
                );
            }
            public static TopupResponse from(pb.topup.Topup.TopupResponseDeleteAt proto) {
                return new TopupResponse(
                        proto.getId(),
                        proto.getCardNumber(),
                        proto.getTopupNo(),
                        proto.getTopupAmount(),
                        proto.getTopupMethod(),
                        proto.getTopupTime(),
                        proto.getCreatedAt(),
                        proto.getUpdatedAt()
                );
            }
        }

        public record FindAllTopupResponse(
                List<TopupResponse> data,
                String status,
                String message) {
            public static FindAllTopupResponse from(pb.topup.TopupQuery.ApiResponsePaginationTopup proto) {
                return new FindAllTopupResponse(
                        proto.getDataList().stream().map(TopupResponse::from).toList(),
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
            public static FindAllTopupResponse from(pb.topup.TopupQuery.ApiResponsePaginationTopupDeleteAt proto) {
                return new FindAllTopupResponse(
                        proto.getDataList().stream().map(TopupResponse::from).toList(),
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
        }

        public record FindByIdTopupResponse(
                TopupResponse data,
                String status,
                String message) {
            public static FindByIdTopupResponse from(pb.topup.Topup.ApiResponseTopup proto) {
                return new FindByIdTopupResponse(
                        proto.hasData() ? TopupResponse.from(proto.getData()) : null,
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
        }

        public record CreateTopupResponse(
                TopupResponse data,
                String status,
                String message) {
            public static CreateTopupResponse from(pb.topup.Topup.ApiResponseTopup proto) {
                return new CreateTopupResponse(
                        proto.hasData() ? TopupResponse.from(proto.getData()) : null,
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
        }

        public record UpdateTopupResponse(
                TopupResponse data,
                String status,
                String message) {
            public static UpdateTopupResponse from(pb.topup.Topup.ApiResponseTopup proto) {
                return new UpdateTopupResponse(
                        proto.hasData() ? TopupResponse.from(proto.getData()) : null,
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
        }

        public record TrashedTopupResponse(
                TopupResponse data,
                String status,
                String message) {
            public static TrashedTopupResponse from(pb.topup.Topup.ApiResponseTopupDeleteAt proto) {
                return new TrashedTopupResponse(
                        proto.hasData() ? TopupResponse.from(proto.getData()) : null,
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
        }

        @org.eclipse.microprofile.graphql.Name("TopupSimpleStatusMessageResponse")
        public record SimpleStatusMessageResponse(
                String status,
                String message) {
            public static SimpleStatusMessageResponse from(pb.topup.TopupCommand.ApiResponseTopupDelete proto) {
                return new SimpleStatusMessageResponse(proto.getStatus(), proto.getMessage());
            }
            public static SimpleStatusMessageResponse from(pb.topup.TopupCommand.ApiResponseTopupAll proto) {
                return new SimpleStatusMessageResponse(proto.getStatus(), proto.getMessage());
            }
        }

        // ── Stats DTOs (from backup, typed per Transfer pattern) ──

        public record TopupMonthAmountResponse(
                String month,
                int totalAmount) {
            public static TopupMonthAmountResponse from(pb.topup.stats.TopupStatsAmount.TopupMonthAmountResponse proto) {
                return new TopupMonthAmountResponse(proto.getMonth(), proto.getTotalAmount());
            }
        }

        public record TopupYearlyAmountResponse(
                String year,
                int totalAmount) {
            public static TopupYearlyAmountResponse from(pb.topup.stats.TopupStatsAmount.TopupYearlyAmountResponse proto) {
                return new TopupYearlyAmountResponse(proto.getYear(), proto.getTotalAmount());
            }
        }

        public record ApiResponseTopupMonthAmount(
                String status,
                String message,
                List<TopupMonthAmountResponse> data) {
            public static ApiResponseTopupMonthAmount from(pb.topup.stats.TopupStatsAmount.ApiResponseTopupMonthAmount proto) {
                return new ApiResponseTopupMonthAmount(
                        proto.getStatus(),
                        proto.getMessage(),
                        proto.getDataList().stream().map(TopupMonthAmountResponse::from).toList());
            }
        }

        public record ApiResponseTopupYearAmount(
                String status,
                String message,
                List<TopupYearlyAmountResponse> data) {
            public static ApiResponseTopupYearAmount from(pb.topup.stats.TopupStatsAmount.ApiResponseTopupYearAmount proto) {
                return new ApiResponseTopupYearAmount(
                        proto.getStatus(),
                        proto.getMessage(),
                        proto.getDataList().stream().map(TopupYearlyAmountResponse::from).toList());
            }
        }

        public record TopupMonthMethodResponse(
                String month,
                String topupMethod,
                int totalTopups,
                int totalAmount) {
            public static TopupMonthMethodResponse from(pb.topup.stats.TopupStatsMethod.TopupMonthMethodResponse proto) {
                return new TopupMonthMethodResponse(proto.getMonth(), proto.getTopupMethod(), proto.getTotalTopups(), proto.getTotalAmount());
            }
        }

        public record TopupYearlyMethodResponse(
                String year,
                String topupMethod,
                int totalTopups,
                int totalAmount) {
            public static TopupYearlyMethodResponse from(pb.topup.stats.TopupStatsMethod.TopupYearlyMethodResponse proto) {
                return new TopupYearlyMethodResponse(proto.getYear(), proto.getTopupMethod(), proto.getTotalTopups(), proto.getTotalAmount());
            }
        }

        public record ApiResponseTopupMonthMethod(
                String status,
                String message,
                List<TopupMonthMethodResponse> data) {
            public static ApiResponseTopupMonthMethod from(pb.topup.stats.TopupStatsMethod.ApiResponseTopupMonthMethod proto) {
                return new ApiResponseTopupMonthMethod(
                        proto.getStatus(),
                        proto.getMessage(),
                        proto.getDataList().stream().map(TopupMonthMethodResponse::from).toList());
            }
        }

        public record ApiResponseTopupYearMethod(
                String status,
                String message,
                List<TopupYearlyMethodResponse> data) {
            public static ApiResponseTopupYearMethod from(pb.topup.stats.TopupStatsMethod.ApiResponseTopupYearMethod proto) {
                return new ApiResponseTopupYearMethod(
                        proto.getStatus(),
                        proto.getMessage(),
                        proto.getDataList().stream().map(TopupYearlyMethodResponse::from).toList());
            }
        }

        public record TopupMonthStatusSuccessResponse(
                String year,
                String month,
                int totalSuccess,
                int totalAmount) {
            public static TopupMonthStatusSuccessResponse from(pb.topup.stats.TopupStatsStatus.TopupMonthStatusSuccessResponse proto) {
                return new TopupMonthStatusSuccessResponse(proto.getYear(), proto.getMonth(), proto.getTotalSuccess(), proto.getTotalAmount());
            }
        }

        public record TopupYearStatusSuccessResponse(
                String year,
                int totalSuccess,
                int totalAmount) {
            public static TopupYearStatusSuccessResponse from(pb.topup.stats.TopupStatsStatus.TopupYearStatusSuccessResponse proto) {
                return new TopupYearStatusSuccessResponse(proto.getYear(), proto.getTotalSuccess(), proto.getTotalAmount());
            }
        }

        public record TopupMonthStatusFailedResponse(
                String year,
                String month,
                int totalFailed,
                int totalAmount) {
            public static TopupMonthStatusFailedResponse from(pb.topup.stats.TopupStatsStatus.TopupMonthStatusFailedResponse proto) {
                return new TopupMonthStatusFailedResponse(proto.getYear(), proto.getMonth(), proto.getTotalFailed(), proto.getTotalAmount());
            }
        }

        public record TopupYearStatusFailedResponse(
                String year,
                int totalFailed,
                int totalAmount) {
            public static TopupYearStatusFailedResponse from(pb.topup.stats.TopupStatsStatus.TopupYearStatusFailedResponse proto) {
                return new TopupYearStatusFailedResponse(proto.getYear(), proto.getTotalFailed(), proto.getTotalAmount());
            }
        }

        public record ApiResponseTopupMonthStatusSuccess(
                String status,
                String message,
                List<TopupMonthStatusSuccessResponse> data) {
            public static ApiResponseTopupMonthStatusSuccess from(pb.topup.stats.TopupStatsStatus.ApiResponseTopupMonthStatusSuccess proto) {
                return new ApiResponseTopupMonthStatusSuccess(
                        proto.getStatus(),
                        proto.getMessage(),
                        proto.getDataList().stream().map(TopupMonthStatusSuccessResponse::from).toList());
            }
        }

        public record ApiResponseTopupYearStatusSuccess(
                String status,
                String message,
                List<TopupYearStatusSuccessResponse> data) {
            public static ApiResponseTopupYearStatusSuccess from(pb.topup.stats.TopupStatsStatus.ApiResponseTopupYearStatusSuccess proto) {
                return new ApiResponseTopupYearStatusSuccess(
                        proto.getStatus(),
                        proto.getMessage(),
                        proto.getDataList().stream().map(TopupYearStatusSuccessResponse::from).toList());
            }
        }

        public record ApiResponseTopupMonthStatusFailed(
                String status,
                String message,
                List<TopupMonthStatusFailedResponse> data) {
            public static ApiResponseTopupMonthStatusFailed from(pb.topup.stats.TopupStatsStatus.ApiResponseTopupMonthStatusFailed proto) {
                return new ApiResponseTopupMonthStatusFailed(
                        proto.getStatus(),
                        proto.getMessage(),
                        proto.getDataList().stream().map(TopupMonthStatusFailedResponse::from).toList());
            }
        }

        public record ApiResponseTopupYearStatusFailed(
                String status,
                String message,
                List<TopupYearStatusFailedResponse> data) {
            public static ApiResponseTopupYearStatusFailed from(pb.topup.stats.TopupStatsStatus.ApiResponseTopupYearStatusFailed proto) {
                return new ApiResponseTopupYearStatusFailed(
                        proto.getStatus(),
                        proto.getMessage(),
                        proto.getDataList().stream().map(TopupYearStatusFailedResponse::from).toList());
            }
        }
}
