package com.sanedge.gateway.dto;

import java.util.List;

public class CardDto {

        public record CardResponse(
                int id,
                int userId,
                String cardNumber,
                String cardType,
                String expireDate,
                String cvv,
                String cardProvider,
                String createdAt,
                String updatedAt) {
            public static CardResponse from(pb.card.Card.CardResponse proto) {
                return new CardResponse(
                        proto.getId(),
                        proto.getUserId(),
                        proto.getCardNumber(),
                        proto.getCardType(),
                        proto.getExpireDate(),
                        proto.getCvv(),
                        proto.getCardProvider(),
                        proto.getCreatedAt(),
                        proto.getUpdatedAt()
                );
            }
            public static CardResponse from(pb.card.Card.CardResponseDeleteAt proto) {
                return new CardResponse(
                        proto.getId(),
                        proto.getUserId(),
                        proto.getCardNumber(),
                        proto.getCardType(),
                        proto.getExpireDate(),
                        proto.getCvv(),
                        proto.getCardProvider(),
                        proto.getCreatedAt(),
                        proto.getUpdatedAt()
                );
            }
        }

        public record CardWithEmailResponse(
                int id,
                int userId,
                String email,
                String cardNumber,
                String cardType,
                String expireDate,
                String cvv,
                String cardProvider,
                String createdAt,
                String updatedAt) {
            public static CardWithEmailResponse from(pb.card.Card.CardWithEmailResponse proto) {
                return new CardWithEmailResponse(
                        proto.getId(),
                        proto.getUserId(),
                        proto.getEmail(),
                        proto.getCardNumber(),
                        proto.getCardType(),
                        proto.getExpireDate(),
                        proto.getCvv(),
                        proto.getCardProvider(),
                        proto.getCreatedAt(),
                        proto.getUpdatedAt()
                );
            }
        }

        public record FindAllCardResponse(
                List<CardResponse> data,
                String status,
                String message) {
            public static FindAllCardResponse from(pb.card.CardQuery.ApiResponsePaginationCard proto) {
                return new FindAllCardResponse(
                        proto.getDataList().stream().map(CardResponse::from).toList(),
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
            public static FindAllCardResponse from(pb.card.CardQuery.ApiResponsePaginationCardDeleteAt proto) {
                return new FindAllCardResponse(
                        proto.getDataList().stream().map(CardResponse::from).toList(),
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
        }

        public record FindByIdCardResponse(
                CardResponse data,
                String status,
                String message) {
            public static FindByIdCardResponse from(pb.card.Card.ApiResponseCard proto) {
                return new FindByIdCardResponse(
                        proto.hasData() ? CardResponse.from(proto.getData()) : null,
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
        }

        public record CreateCardRequest(
                int userId,
                String cardType,
                String expireDate,
                String cvv,
                String cardProvider) {}

        public record CreateCardResponse(
                CardResponse data,
                String status,
                String message) {
            public static CreateCardResponse from(pb.card.Card.ApiResponseCard proto) {
                return new CreateCardResponse(
                        proto.hasData() ? CardResponse.from(proto.getData()) : null,
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
        }

        public record UpdateCardRequest(
                int userId,
                String cardType,
                String expireDate,
                String cvv,
                String cardProvider) {}

        public record UpdateCardResponse(
                CardResponse data,
                String status,
                String message) {
            public static UpdateCardResponse from(pb.card.Card.ApiResponseCard proto) {
                return new UpdateCardResponse(
                        proto.hasData() ? CardResponse.from(proto.getData()) : null,
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
        }

        public record TrashedCardResponse(
                CardResponse data,
                String status,
                String message) {
            public static TrashedCardResponse from(pb.card.Card.ApiResponseCardDeleteAt proto) {
                return new TrashedCardResponse(
                        proto.hasData() ? CardResponse.from(proto.getData()) : null,
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
        }

        @org.eclipse.microprofile.graphql.Name("CardSimpleStatusMessageResponse")
        public record SimpleStatusMessageResponse(
                String status,
                String message) {
            public static SimpleStatusMessageResponse from(pb.card.CardCommand.ApiResponseCardDelete proto) {
                return new SimpleStatusMessageResponse(proto.getStatus(), proto.getMessage());
            }
            public static SimpleStatusMessageResponse from(pb.card.CardCommand.ApiResponseCardAll proto) {
                return new SimpleStatusMessageResponse(proto.getStatus(), proto.getMessage());
            }
        }

        // ── Stats DTOs (from backup, typed per backup pattern) ──

        public record CardResponseMonthlyAmount(
                String month,
                long totalAmount) {
            public static CardResponseMonthlyAmount from(pb.card.Card.CardResponseMonthlyAmount proto) {
                return new CardResponseMonthlyAmount(proto.getMonth(), proto.getTotalAmount());
            }
        }

        public record CardResponseYearlyAmount(
                String year,
                long totalAmount) {
            public static CardResponseYearlyAmount from(pb.card.Card.CardResponseYearlyAmount proto) {
                return new CardResponseYearlyAmount(proto.getYear(), proto.getTotalAmount());
            }
        }

        public record ApiResponseMonthlyAmount(
                String status,
                String message,
                List<CardResponseMonthlyAmount> data) {
            public static ApiResponseMonthlyAmount from(pb.card.Card.ApiResponseMonthlyAmount proto) {
                return new ApiResponseMonthlyAmount(
                        proto.getStatus(),
                        proto.getMessage(),
                        proto.getDataList().stream().map(CardResponseMonthlyAmount::from).toList());
            }
        }

        public record ApiResponseYearlyAmount(
                String status,
                String message,
                List<CardResponseYearlyAmount> data) {
            public static ApiResponseYearlyAmount from(pb.card.Card.ApiResponseYearlyAmount proto) {
                return new ApiResponseYearlyAmount(
                        proto.getStatus(),
                        proto.getMessage(),
                        proto.getDataList().stream().map(CardResponseYearlyAmount::from).toList());
            }
        }

        public record CardResponseMonthlyBalance(
                String month,
                long totalBalance) {
            public static CardResponseMonthlyBalance from(pb.card.stats.CardStatsBalance.CardResponseMonthlyBalance proto) {
                return new CardResponseMonthlyBalance(proto.getMonth(), proto.getTotalBalance());
            }
        }

        public record CardResponseYearlyBalance(
                String year,
                long totalBalance) {
            public static CardResponseYearlyBalance from(pb.card.stats.CardStatsBalance.CardResponseYearlyBalance proto) {
                return new CardResponseYearlyBalance(proto.getYear(), proto.getTotalBalance());
            }
        }

        public record ApiResponseMonthlyBalance(
                String status,
                String message,
                List<CardResponseMonthlyBalance> data) {
            public static ApiResponseMonthlyBalance from(pb.card.stats.CardStatsBalance.ApiResponseMonthlyBalance proto) {
                return new ApiResponseMonthlyBalance(
                        proto.getStatus(),
                        proto.getMessage(),
                        proto.getDataList().stream().map(CardResponseMonthlyBalance::from).toList());
            }
        }

        public record ApiResponseYearlyBalance(
                String status,
                String message,
                List<CardResponseYearlyBalance> data) {
            public static ApiResponseYearlyBalance from(pb.card.stats.CardStatsBalance.ApiResponseYearlyBalance proto) {
                return new ApiResponseYearlyBalance(
                        proto.getStatus(),
                        proto.getMessage(),
                        proto.getDataList().stream().map(CardResponseYearlyBalance::from).toList());
            }
        }

        public record CardResponseDashboard(
                long totalBalance,
                long totalTopup,
                long totalWithdraw,
                long totalTransaction,
                long totalTransfer) {
            public static CardResponseDashboard from(pb.card.CardDashboard.CardResponseDashboard proto) {
                return new CardResponseDashboard(
                        proto.getTotalBalance(),
                        proto.getTotalTopup(),
                        proto.getTotalWithdraw(),
                        proto.getTotalTransaction(),
                        proto.getTotalTransfer());
            }
        }

        public record CardResponseDashboardCardNumber(
                long totalBalance,
                long totalTopup,
                long totalWithdraw,
                long totalTransaction,
                long totalTransferSend,
                long totalTransferReceiver) {
            public static CardResponseDashboardCardNumber from(pb.card.CardDashboard.CardResponseDashboardCardNumber proto) {
                return new CardResponseDashboardCardNumber(
                        proto.getTotalBalance(),
                        proto.getTotalTopup(),
                        proto.getTotalWithdraw(),
                        proto.getTotalTransaction(),
                        proto.getTotalTransferSend(),
                        proto.getTotalTransferReceiver());
            }
        }

        public record ApiResponseDashboardCard(
                String status,
                String message,
                CardResponseDashboard data) {
            public static ApiResponseDashboardCard from(pb.card.CardDashboard.ApiResponseDashboardCard proto) {
                return new ApiResponseDashboardCard(
                        proto.getStatus(),
                        proto.getMessage(),
                        proto.hasData() ? CardResponseDashboard.from(proto.getData()) : null);
            }
        }

        public record ApiResponseDashboardCardNumber(
                String status,
                String message,
                CardResponseDashboardCardNumber data) {
            public static ApiResponseDashboardCardNumber from(pb.card.CardDashboard.ApiResponseDashboardCardNumber proto) {
                return new ApiResponseDashboardCardNumber(
                        proto.getStatus(),
                        proto.getMessage(),
                        proto.hasData() ? CardResponseDashboardCardNumber.from(proto.getData()) : null);
            }
        }
}
