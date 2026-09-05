package com.sanedge.saldo.repository;

import com.sanedge.saldo.entity.SaldoMutationOperation;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SaldoMutationOperationRepository implements PanacheRepository<SaldoMutationOperation> {
    public Uni<SaldoMutationOperation> findByOperationKey(String operationKey) {
        return find("operationKey = ?1", operationKey).firstResult();
    }
}
