package com.sanedge.saldo.domain.response;

import com.sanedge.saldo.entity.Saldo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SaldoResponseDeleteAt {
        private Long id;
        private String cardNumber;
        private Long totalBalance;
        private Long withdrawAmount;
        private String withdrawTime;
        private String createdAt;
        private String updatedAt;
        private String deletedAt;

        public static SaldoResponseDeleteAt from(Saldo saldo) {
                return SaldoResponseDeleteAt.builder()
                                .id(saldo.getSaldoId())
                                .cardNumber(saldo.getCardNumber())
                                .totalBalance(saldo.getTotalBalance().longValue())
                                .withdrawAmount(saldo.getWithdrawAmount().longValue())
                                .withdrawTime(
                                                saldo.getWithdrawTime() != null ? saldo.getWithdrawTime().toString()
                                                                : null)
                                .createdAt(
                                                saldo.getCreatedAt() != null ? saldo.getCreatedAt().toString() : null)
                                .updatedAt(
                                                saldo.getUpdatedAt() != null ? saldo.getUpdatedAt().toString() : null)
                                .deletedAt(
                                                saldo.getDeletedAt() != null ? saldo.getDeletedAt().toString() : null)
                                .build();
        }
}
