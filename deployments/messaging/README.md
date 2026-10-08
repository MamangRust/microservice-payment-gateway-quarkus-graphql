# Distributed Event Fabric and Kafka Orchestration

This document details the event streaming infrastructure and Kafka messaging patterns used across the Distributed Microservice Payment Gateway.

## Kafka Cluster Topology

The system utilizes a 3-broker Kafka cluster operating in KRaft mode, eliminating the need for an external Zookeeper quorum and ensuring high availability for all event logs.

### Messaging Plane Organization

The messaging fabric is segmented into domain-specific topics to ensure data isolation and granular scalability.

1. Transactional Stream (`transaction.events`)
   - Emits all state changes for financial transactions.
   - Utilized by the analytical engine and notification services.

2. Security Stream (`security.fraud.signals`)
   - Propagates behavioral risk scores and fraud alerts from the AI Security engine.

3. Accounting Stream (`balance.delta`)
   - Captures changes in user and merchant balances for real-time ledger reconciliation.

## Operational Invariants

- KRaft Quorum: High-availability controller layer providing consensus and leader election.
- Replication Factor: Critical topics are configured with a replication factor of 3 to ensure zero data loss during broker failure.
- Partitioning Strategy: Keys are utilized to ensure that events for the same entity (e.g., UserID) are processed in order by consumers.

## Development and Integration

Microservices interact with the event bus using standardized clients with support for:
- Automatic retries with exponential backoff.
- Idempotent producer configurations.
- Schema registry compatibility (where applicable).

## Monitoring and Cluster Health

The event fabric is monitored via the Unified OTLP pipeline with specialized metrics for:
- Under-replicated partitions.
- Consumer group lag percentiles.
- Broker throughput and disk utilization.
- Leader election frequency.
