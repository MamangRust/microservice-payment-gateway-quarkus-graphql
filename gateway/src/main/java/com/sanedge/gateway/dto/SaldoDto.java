package com.sanedge.gateway.dto;

import java.util.List;

public class SaldoDto {

    public record SaldoResponse(
            int saldoId,
            String cardNumber,
            int totalBalance,
            String withdrawTime,
            int withdrawAmount,
            String createdAt,
            String updatedAt) {
        public static SaldoResponse from(pb.saldo.Saldo.SaldoResponse proto) {
            return new SaldoResponse(
                    proto.getSaldoId(),
                    proto.getCardNumber(),
                    proto.getTotalBalance(),
                    proto.getWithdrawTime(),
                    proto.getWithdrawAmount(),
                    proto.getCreatedAt(),
                    proto.getUpdatedAt());
        }
        public static SaldoResponse from(pb.saldo.Saldo.SaldoResponseDeleteAt proto) {
            return new SaldoResponse(
                    proto.getSaldoId(),
                    proto.getCardNumber(),
                    proto.getTotalBalance(),
                    proto.getWithdrawTime(),
                    proto.getWithdrawAmount(),
                    proto.getCreatedAt(),
                    proto.getUpdatedAt());
        }
    }

    public record FindAllSaldoResponse(
            List<SaldoResponse> data,
            String status,
            String message) {
        public static FindAllSaldoResponse from(pb.saldo.SaldoQuery.ApiResponsePaginationSaldo proto) {
            return new FindAllSaldoResponse(
                    proto.getDataList().stream().map(SaldoResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage());
        }
        public static FindAllSaldoResponse from(pb.saldo.SaldoQuery.ApiResponsePaginationSaldoDeleteAt proto) {
            return new FindAllSaldoResponse(
                    proto.getDataList().stream().map(SaldoResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage());
        }
    }

    public record FindByIdSaldoResponse(
            SaldoResponse data,
            String status,
            String message) {
        public static FindByIdSaldoResponse from(pb.saldo.Saldo.ApiResponseSaldo proto) {
            return new FindByIdSaldoResponse(
                    proto.hasData() ? SaldoResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage());
        }
    }

    public record CreateSaldoRequest(
            String cardNumber,
            int totalBalance) {
    }

    public record CreateSaldoResponse(
            SaldoResponse data,
            String status,
            String message) {
        public static CreateSaldoResponse from(pb.saldo.Saldo.ApiResponseSaldo proto) {
            return new CreateSaldoResponse(
                    proto.hasData() ? SaldoResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage());
        }
    }

    public record UpdateSaldoRequest(
            String cardNumber,
            int totalBalance) {
    }

    public record UpdateSaldoResponse(
            SaldoResponse data,
            String status,
            String message) {
        public static UpdateSaldoResponse from(pb.saldo.Saldo.ApiResponseSaldo proto) {
            return new UpdateSaldoResponse(
                    proto.hasData() ? SaldoResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage());
        }
    }

    public record UpdateSaldoWithdrawRequest(
            String cardNumber,
            int totalBalance,
            String withdrawTime,
            int withdrawAmount) {
    }

    public record UpdateSaldoBalanceRequest(
            String cardNumber,
            int totalBalance) {
    }

    public record TrashedSaldoResponse(
            SaldoResponse data,
            String status,
            String message) {
        public static TrashedSaldoResponse from(pb.saldo.Saldo.ApiResponseSaldoDeleteAt proto) {
            return new TrashedSaldoResponse(
                    proto.hasData() ? SaldoResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage());
        }
    }

    @org.eclipse.microprofile.graphql.Name("SaldoSimpleStatusMessageResponse")
    public record SimpleStatusMessageResponse(
            String status,
            String message) {
        public static SimpleStatusMessageResponse from(pb.saldo.SaldoCommand.ApiResponseSaldoDelete proto) {
            return new SimpleStatusMessageResponse(proto.getStatus(), proto.getMessage());
        }
        public static SimpleStatusMessageResponse from(pb.saldo.SaldoCommand.ApiResponseSaldoAll proto) {
            return new SimpleStatusMessageResponse(proto.getStatus(), proto.getMessage());
        }
    }

    // ── Stats DTOs (from backup, typed per backup pattern) ──

    public record SaldoMonthBalanceResponse(
            String month,
            int totalBalance) {
        public static SaldoMonthBalanceResponse from(pb.saldo.stats.SaldoStatsBalance.SaldoMonthBalanceResponse proto) {
            return new SaldoMonthBalanceResponse(proto.getMonth(), proto.getTotalBalance());
        }
    }

    public record SaldoYearBalanceResponse(
            String year,
            int totalBalance) {
        public static SaldoYearBalanceResponse from(pb.saldo.stats.SaldoStatsBalance.SaldoYearBalanceResponse proto) {
            return new SaldoYearBalanceResponse(proto.getYear(), proto.getTotalBalance());
        }
    }

    public record ApiResponseMonthSaldoBalances(
            String status,
            String message,
            List<SaldoMonthBalanceResponse> data) {
        public static ApiResponseMonthSaldoBalances from(pb.saldo.stats.SaldoStatsBalance.ApiResponseMonthSaldoBalances proto) {
            return new ApiResponseMonthSaldoBalances(
                    proto.getStatus(),
                    proto.getMessage(),
                    proto.getDataList().stream().map(SaldoMonthBalanceResponse::from).toList());
        }
    }

    public record ApiResponseYearSaldoBalances(
            String status,
            String message,
            List<SaldoYearBalanceResponse> data) {
        public static ApiResponseYearSaldoBalances from(pb.saldo.stats.SaldoStatsBalance.ApiResponseYearSaldoBalances proto) {
            return new ApiResponseYearSaldoBalances(
                    proto.getStatus(),
                    proto.getMessage(),
                    proto.getDataList().stream().map(SaldoYearBalanceResponse::from).toList());
        }
    }

    public record SaldoMonthTotalBalanceResponse(
            String month,
            String year,
            int totalBalance) {
        public static SaldoMonthTotalBalanceResponse from(pb.saldo.stats.SaldoStatsTotal.SaldoMonthTotalBalanceResponse proto) {
            return new SaldoMonthTotalBalanceResponse(proto.getMonth(), proto.getYear(), proto.getTotalBalance());
        }
    }

    public record SaldoYearTotalBalanceResponse(
            String year,
            int totalBalance) {
        public static SaldoYearTotalBalanceResponse from(pb.saldo.stats.SaldoStatsTotal.SaldoYearTotalBalanceResponse proto) {
            return new SaldoYearTotalBalanceResponse(proto.getYear(), proto.getTotalBalance());
        }
    }

    public record ApiResponseMonthTotalSaldo(
            String status,
            String message,
            List<SaldoMonthTotalBalanceResponse> data) {
        public static ApiResponseMonthTotalSaldo from(pb.saldo.stats.SaldoStatsTotal.ApiResponseMonthTotalSaldo proto) {
            return new ApiResponseMonthTotalSaldo(
                    proto.getStatus(),
                    proto.getMessage(),
                    proto.getDataList().stream().map(SaldoMonthTotalBalanceResponse::from).toList());
        }
    }

    public record ApiResponseYearTotalSaldo(
            String status,
            String message,
            List<SaldoYearTotalBalanceResponse> data) {
        public static ApiResponseYearTotalSaldo from(pb.saldo.stats.SaldoStatsTotal.ApiResponseYearTotalSaldo proto) {
            return new ApiResponseYearTotalSaldo(
                    proto.getStatus(),
                    proto.getMessage(),
                    proto.getDataList().stream().map(SaldoYearTotalBalanceResponse::from).toList());
        }
    }
}
