package com.sanedge.merchant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "merchant_documents")
public class MerchantDocument extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_id")
    public Long documentId;

    @Column(name = "merchant_id", nullable = false)
    public Integer merchantId;

    @Column(name = "document_type", nullable = false)
    public String documentType;

    @Column(name = "document_url", nullable = false)
    public String documentUrl;

    @Column(nullable = false)
    public String status = "PENDING";

    @Column
    public String note;
}
