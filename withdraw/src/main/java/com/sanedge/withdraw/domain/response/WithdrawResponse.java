package com.sanedge.withdraw.domain.response;

import com.sanedge.withdraw.entity.Withdraw;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawResponse {
    private Long id;
    private String withdrawNo;
    private String cardNumber;
    private Long withdrawAmount;
    private String withdrawTime;
    private String createdAt;
    private String updatedAt;

    public static WithdrawResponse from(Withdraw w) {
        return WithdrawResponse.builder()
                .id(w.getWithdrawId())
                .withdrawNo(w.getWithdrawNo().toString())
                .cardNumber(w.getCardNumber())
                .withdrawAmount(w.getWithdrawAmount().longValue())
                .withdrawTime(w.getWithdrawTime() != null ? w.getWithdrawTime().toString() : null)
                .createdAt(w.getCreatedAt() != null ? w.getCreatedAt().toString() : null)
                .updatedAt(w.getUpdatedAt() != null ? w.getUpdatedAt().toString() : null)
                .build();
    }
}