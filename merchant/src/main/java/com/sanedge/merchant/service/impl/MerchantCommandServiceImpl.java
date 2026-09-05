package com.sanedge.merchant.service.impl;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sanedge.common.config.RedisService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.enums.Status;
import com.sanedge.common.exception.InvalidRequestException;
import com.sanedge.common.exception.ResourceAlreadyExistsException;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.common.utils.ApiKeyGenerator;
import com.sanedge.merchant.domain.requests.CreateMerchantRequest;
import com.sanedge.merchant.domain.requests.UpdateMerchantRequest;
import com.sanedge.merchant.domain.response.MerchantResponse;
import com.sanedge.merchant.domain.response.MerchantResponseDeleteAt;
import com.sanedge.merchant.entity.Merchant;
import com.sanedge.merchant.entity.Outbox;
import com.sanedge.merchant.repository.MerchantCommandRepository;
import com.sanedge.merchant.repository.MerchantQueryRepository;
import com.sanedge.merchant.repository.OutboxRepository;
import com.sanedge.merchant.service.MerchantCommandService;

import io.opentelemetry.api.common.Attributes;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import pb.user.UserQueryService;

@ApplicationScoped
public class MerchantCommandServiceImpl implements MerchantCommandService {
        private static final Logger logger = LoggerFactory.getLogger(MerchantCommandServiceImpl.class);

        private final UserQueryService userQueryService;
        private final MerchantQueryRepository merchantQueryRepository;
        private final MerchantCommandRepository merchantCommandRepository;
        private final RedisService redisService;
        private final TracingMetrics tracingMetrics;
        private final OutboxRepository outboxRepository;

        @Inject
        public MerchantCommandServiceImpl(
                        @GrpcClient("user") UserQueryService userQueryService,
                        MerchantQueryRepository merchantQueryRepository,
                        MerchantCommandRepository merchantCommandRepository,
                        RedisService redisService,
                        TracingMetrics tracingMetrics,
                        OutboxRepository outboxRepository) {
                this.userQueryService = userQueryService;
                this.merchantQueryRepository = merchantQueryRepository;
                this.merchantCommandRepository = merchantCommandRepository;
                this.redisService = redisService;
                this.tracingMetrics = tracingMetrics;
                this.outboxRepository = outboxRepository;
        }

        /**
         * Writes a merchant.created stats event into the transactional outbox
         * (same DB transaction as the merchant persist). Relayed to Kafka by
         * {@code OutboxPublisher}.
         */
        private Uni<Void> persistOutboxEvent(String merchantId, Merchant merchant) {
                Outbox outbox = new Outbox();
                outbox.setDomain("merchant");
                outbox.setTopic("stats.payment.merchant.event");
                outbox.setEventKey(merchantId);
                outbox.setEventId(java.util.UUID.randomUUID().toString());
                JsonObject payload = new JsonObject()
                                .put("merchant_id", merchant.getMerchantId())
                                .put("user_id", merchant.getUserId())
                                .put("name", merchant.getName())
                                .put("status", merchant.getStatus() != null ? merchant.getStatus().name() : null);
                outbox.setPayload(com.sanedge.common.event.EventEnvelope
                                .withDefaults(payload, "merchant.created")
                                .encode());
                return outboxRepository.persist(outbox).replaceWithVoid();
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<MerchantResponse>> createMerchant(CreateMerchantRequest req) {
                Attributes attrs = Attributes.builder()
                                .put("merchant.name", req.getName())
                                .put("user.id", req.getUserId())
                                .build();
                logger.info("Creating merchant | Name: {}, UserId: {}", req.getName(), req.getUserId());

                return tracingMetrics.traceAndMeasure("createMerchant", "create_merchant", attrs, () -> {
                        return userQueryService
                                        .findById(pb.user.User.FindByIdUserRequest.newBuilder()
                                                        .setId(req.getUserId().intValue()).build())
                                        .chain(response -> {
                                                if (response == null || !response.hasData()) {
                                                        logger.error("User not found with id {}", req.getUserId());
                                                        throw new ResourceNotFoundException("User not found");
                                                }
                                                return merchantQueryRepository.existsByName(req.getName());
                                        })
                                        .chain(nameExists -> {
                                                if (nameExists) {
                                                        logger.error("Merchant name already taken | Name: {}",
                                                                        req.getName());
                                                        throw new ResourceAlreadyExistsException(
                                                                        "Merchant name already taken");
                                                }

                                                String apiKey = ApiKeyGenerator.generateApiKey();
                                                UUID merchantNo = UUID.randomUUID();

                                                Merchant merchant = new Merchant();
                                                merchant.setName(req.getName());
                                                merchant.setMerchantNo(merchantNo);
                                                merchant.setUserId(req.getUserId().intValue());
                                                merchant.setApiKey(apiKey);
                                                merchant.setStatus(Status.PENDING);                return merchantCommandRepository.persist(merchant)
                                .chain(savedMerchant -> persistOutboxEvent(
                                                String.valueOf(merchant.getMerchantId()), savedMerchant)
                                                .replaceWith(savedMerchant))
                                .chain(savedMerchant -> {
                                        logger.info("Merchant created successfully | Id: {}, ApiKey: {}",
                                                        merchant.getMerchantId(),
                                                        apiKey);
                                        return Uni.createFrom().item(ApiResponse
                                                        .success("Merchant created successfully",
                                                                        MerchantResponse.from(
                                                                                        merchant)));
                                });
                                        });
                });
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<MerchantResponse>> updateMerchant(UpdateMerchantRequest req) {
                Attributes attrs = Attributes.builder().put("merchant.id", req.getMerchantId()).build();
                logger.info("Updating merchant | Id: {}", req.getMerchantId());

                return tracingMetrics.traceAndMeasure("updateMerchant", "update_merchant", attrs, () -> {
                        return merchantQueryRepository.findMerchantById(req.getMerchantId())
                                        .chain(merchant -> {
                                                if (merchant == null) {
                                                        logger.error("Merchant not found with id {}",
                                                                        req.getMerchantId());
                                                        throw new ResourceNotFoundException("Merchant not found");
                                                }

                                                Uni<Void> userCheckUni = Uni.createFrom().nullItem();
                                                if (req.getUserId() != null) {
                                                        userCheckUni = userQueryService
                                                                        .findById(pb.user.User.FindByIdUserRequest
                                                                                        .newBuilder()
                                                                                        .setId(req.getUserId()
                                                                                                        .intValue())
                                                                                        .build())
                                                                        .chain(response -> {
                                                                                if (response == null || !response
                                                                                                .hasData()) {
                                                                                        logger.error("User not found with id {}",
                                                                                                        req.getUserId());
                                                                                        throw new ResourceNotFoundException(
                                                                                                        "User not found");
                                                                                }
                                                                                merchant.setUserId(req.getUserId()
                                                                                                .intValue());
                                                                                return Uni.createFrom().nullItem();
                                                                        });
                                                }

                                                return userCheckUni.chain(v -> {
                                                        merchant.setName(req.getName());
                                                        merchant.setStatus(
                                                                        Status.valueOf(req.getStatus().toUpperCase()));

                                                        return merchantCommandRepository.persist(merchant)
                                                                        .chain(savedMerchant -> {
                                                                                String cacheIdKey = "merchant:id:"
                                                                                                + req.getMerchantId();
                                                                                String cacheApiKey = "merchant:apikey:"
                                                                                                + merchant.getApiKey();
                                                                                String cacheUserKey = "merchant:user:"
                                                                                                + merchant.getUserId();

                                                                                return Uni.combine().all().unis(
                                                                                                redisService.deleteReactive(
                                                                                                                cacheIdKey),
                                                                                                redisService.deleteReactive(
                                                                                                                cacheApiKey),
                                                                                                redisService.deleteReactive(
                                                                                                                cacheUserKey))
                                                                                                .asTuple().map(v2 -> {
                                                                                                        logger.info("Merchant updated successfully | Id: {}",
                                                                                                                        req.getMerchantId());
                                                                                                        return ApiResponse
                                                                                                                        .success("Merchant updated successfully",
                                                                                                                                        MerchantResponse.from(
                                                                                                                                                        merchant));
                                                                                                });
                                                                        });
                                                });
                                        });
                });
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<MerchantResponseDeleteAt>> trashMerchant(Long id) {
                Attributes attrs = Attributes.builder().put("merchant.id", id).build();
                logger.info("Trashing merchant id={}", id);

                return tracingMetrics.traceAndMeasure("trashMerchant", "trash_merchant", attrs, () -> {
                        return merchantCommandRepository.trashed(id)
                                        .chain(merchant -> {
                                                if (merchant == null) {
                                                        logger.error("Merchant not found with id {}", id);
                                                        throw new ResourceNotFoundException("Merchant not found");
                                                }

                                                String cacheIdKey = "merchant:id:" + id;
                                                String cacheApiKey = "merchant:apikey:" + merchant.getApiKey();
                                                String cacheUserKey = "merchant:user:" + merchant.getUserId();

                                                return Uni.combine().all().unis(
                                                                redisService.deleteReactive(cacheIdKey),
                                                                redisService.deleteReactive(cacheApiKey),
                                                                redisService.deleteReactive(cacheUserKey)).asTuple()
                                                                .map(v2 -> {
                                                                        logger.info("Merchant trashed successfully | Id: {}",
                                                                                        id);
                                                                        return ApiResponse.success(
                                                                                        "Merchant trashed successfully",
                                                                                        MerchantResponseDeleteAt.from(
                                                                                                        merchant));
                                                                });
                                        });
                });
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<MerchantResponseDeleteAt>> restoreMerchant(Long id) {
                Attributes attrs = Attributes.builder().put("merchant.id", id).build();
                logger.info("Restoring merchant id={}", id);

                return tracingMetrics.traceAndMeasure("restoreMerchant", "restore_merchant", attrs, () -> {
                        return merchantCommandRepository.restore(id)
                                        .chain(merchant -> {
                                                if (merchant == null) {
                                                        logger.error("Merchant restore failed - merchant not found or must be trashed first with id {}", id);
                                                        throw new InvalidRequestException("Merchant not found or must be trashed first");
                                                }

                                                String cacheIdKey = "merchant:id:" + id;
                                                String cacheApiKey = "merchant:apikey:" + merchant.getApiKey();
                                                String cacheUserKey = "merchant:user:" + merchant.getUserId();

                                                return Uni.combine().all().unis(
                                                                redisService.deleteReactive(cacheIdKey),
                                                                redisService.deleteReactive(cacheApiKey),
                                                                redisService.deleteReactive(cacheUserKey)).asTuple()
                                                                .map(v2 -> {
                                                                        logger.info("Merchant restored successfully | Id: {}",
                                                                                        id);
                                                                        return ApiResponse.success(
                                                                                        "Merchant restored successfully",
                                                                                        MerchantResponseDeleteAt.from(
                                                                                                        merchant));
                                                                });
                                        });
                });
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<Boolean>> deleteMerchant(Long id) {
                Attributes attrs = Attributes.builder().put("merchant.id", id).build();
                logger.info("Permanently deleting merchant id={}", id);

                return tracingMetrics.traceAndMeasure("deleteMerchant", "delete_merchant", attrs, () -> {
                        return merchantCommandRepository.deletePermanent(id)
                                        .chain(deleted -> {
                                                if (!deleted) {
                                                        logger.error("Permanent delete failed - merchant not found or must be trashed before permanent deletion with id {}", id);
                                                        throw new InvalidRequestException("Merchant not found or must be trashed before permanent deletion");
                                                }

                                                String cacheIdKey = "merchant:id:" + id;
                                                return redisService.deleteReactive(cacheIdKey)
                                                                .map(v2 -> {
                                                                        logger.info("Merchant permanently deleted | Id: {}",
                                                                                        id);
                                                                        return ApiResponse.success("Merchant permanently deleted", true);
                                                                });
                                        });
                });
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<Boolean>> restoreAll() {
                logger.info("Restoring ALL trashed merchants");

                return tracingMetrics.traceAndMeasure("restoreAllMerchants", "restore_all_merchants", () -> {
                        return merchantCommandRepository.restoreAllDeleted()
                                        .map(restored -> {
                                                if (!restored) {
                                                        throw new ResourceNotFoundException("No trashed merchants found");
                                                }
                                                logger.info("Restored all trashed merchants");
                                                return ApiResponse.success("Restored all trashed merchants", true);
                                        });
                });
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<Boolean>> deleteAll() {
                logger.info("Permanently deleting ALL trashed merchants");

                return tracingMetrics.traceAndMeasure("deleteAllMerchants", "delete_all_merchants", () -> {
                        return merchantCommandRepository.deleteAllDeleted()
                                        .map(deleted -> {
                                                if (!deleted) {
                                                        throw new ResourceNotFoundException("No trashed merchants found");
                                                }
                                                logger.info("Deleted all trashed merchants");
                                                return ApiResponse.success("Deleted all trashed merchants", true);
                                        });
                });
        }
}