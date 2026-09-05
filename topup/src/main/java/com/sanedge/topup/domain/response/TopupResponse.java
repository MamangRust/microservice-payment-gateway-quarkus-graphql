package com.sanedge.topup.domain.response;

import com.sanedge.topup.entity.Topup;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopupResponse {
    private Long id;
    private String cardNumber;
    private String topupNo;
    private Long topupAmount;
    private String topupMethod;
    private String topupTime;
    private String createdAt;
    private String updatedAt;

    public static TopupResponse from(Topup topup) {
        return TopupResponse.builder()
                .id(topup.getTopupId())
                .cardNumber(topup.getCardNumber())
                .topupNo(topup.getTopupNo().toString())
                .topupAmount(topup.getTopupAmount().longValue())
                .topupMethod(topup.getTopupMethod())
                .topupTime(topup.getTopupTime() != null ? topup.getTopupTime().toString() : null)
                .createdAt(topup.getCreatedAt() != null ? topup.getCreatedAt().toString() : null)
                .updatedAt(topup.getUpdatedAt() != null ? topup.getUpdatedAt().toString() : null)
                .build();
    }
}