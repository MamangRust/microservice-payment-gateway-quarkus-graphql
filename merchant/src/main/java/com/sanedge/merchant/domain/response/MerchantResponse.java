package com.sanedge.merchant.domain.response;


import com.sanedge.merchant.entity.Merchant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantResponse {
    private Long id;
    private String name;
    private Long userId;
    private String apiKey;
    private String status;
    private String createdAt;
    private String updatedAt;

    public static MerchantResponse from(Merchant merchant) {
        return MerchantResponse.builder()
                .id(merchant.getMerchantId())
                .name(merchant.getName())
                .userId(merchant.getUserId().longValue())
                .apiKey(merchant.getApiKey())
                .status(merchant.getStatus() != null ? merchant.getStatus().name() : null)
                .createdAt(merchant.getCreatedAt() != null ? merchant.getCreatedAt().toString() : null)
                .updatedAt(merchant.getUpdatedAt() != null ? merchant.getUpdatedAt().toString() : null)
                .build();
    }

}