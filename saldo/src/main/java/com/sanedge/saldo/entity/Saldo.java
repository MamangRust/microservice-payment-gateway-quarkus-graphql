package com.sanedge.saldo.entity;

import java.sql.Timestamp;

import com.sanedge.saldo.entity.BaseModel;

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
@Table(name = "saldos")
public class Saldo extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "saldo_id")
    public Long saldoId;

    @Column(name = "card_number", nullable = false)
    public String cardNumber;

    @Column(name = "total_balance", nullable = false)
    public Integer totalBalance;

    @Column(name = "withdraw_amount", nullable = true)
    public Integer withdrawAmount;

    @Column(name = "withdraw_time", nullable = true)
    public Timestamp withdrawTime;
}
