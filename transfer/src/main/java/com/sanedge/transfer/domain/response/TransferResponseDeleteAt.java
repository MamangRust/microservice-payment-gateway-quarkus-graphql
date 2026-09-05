package com.sanedge.transfer.domain.response;

import com.sanedge.transfer.entity.Transfer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferResponseDeleteAt {
    private Long id;
    private String transferNo;
    private String transferFrom;
    private String transferTo;
    private Long transferAmount;
    private String transferTime;
    private String createdAt;
    private String updatedAt;
    private String deletedAt;

    public static TransferResponseDeleteAt from(Transfer t) {
        return TransferResponseDeleteAt.builder()
                .id(t.getTransferId())
                .transferNo(t.getTransferNo().toString())
                .transferFrom(t.getTransferFrom())
                .transferTo(t.getTransferTo())
                .transferAmount(t.getTransferAmount().longValue())
                .transferTime(t.getTransferTime() != null ? t.getTransferTime().toString() : null)
                .createdAt(t.getCreatedAt() != null ? t.getCreatedAt().toString() : null)
                .updatedAt(t.getUpdatedAt() != null ? t.getUpdatedAt().toString() : null)
                .deletedAt(t.getDeletedAt() != null ? t.getDeletedAt().toString() : null)
                .build();
    }
}