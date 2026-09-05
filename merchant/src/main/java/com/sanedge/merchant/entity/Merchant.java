package com.sanedge.merchant.entity;

import java.util.UUID;
import com.sanedge.common.enums.Status;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "merchants")
public class Merchant extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "merchant_id")
    public Long merchantId;

    @Column(name = "merchant_no", nullable = false, unique = true)
    public UUID merchantNo;

    @Column(nullable = false)
    public String name;

    @Column(name = "api_key", unique = true, nullable = false)
    public String apiKey;

    @Column(name = "user_id", nullable = false)
    public Integer userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public Status status = Status.PENDING;
}