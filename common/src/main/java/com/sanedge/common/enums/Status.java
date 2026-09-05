package com.sanedge.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Status {
    PENDING("pending"),
    SUCCESS("success"),
    FAILED("failed"),
    COMPENSATION_REQUIRED("compensation_required"),
    COMPENSATED("compensated");

    private final String value;
}
