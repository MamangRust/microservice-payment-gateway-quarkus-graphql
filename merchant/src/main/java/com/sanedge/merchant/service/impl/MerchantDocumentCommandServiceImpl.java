package com.sanedge.merchant.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sanedge.common.config.RedisService;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.merchant.domain.requests.CreateMerchantDocumentRequest;
import com.sanedge.merchant.domain.requests.UpdateMerchantDocumentRequest;
import com.sanedge.merchant.domain.requests.UpdateMerchantDocumentStatus;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.merchant.domain.response.MerchantDocumentResponse;
import com.sanedge.merchant.domain.response.MerchantDocumentResponseDeleteAt;
import com.sanedge.merchant.entity.MerchantDocument;
import com.sanedge.common.exception.InvalidRequestException;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.merchant.repository.MerchantQueryRepository;
import com.sanedge.merchant.repository.MerchantDocumentCommandRepository;
import com.sanedge.merchant.repository.MerchantDocumentQueryRepository;
import com.sanedge.merchant.service.MerchantDocumentCommandService;

import io.opentelemetry.api.common.Attributes;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MerchantDocumentCommandServiceImpl implements MerchantDocumentCommandService {
    private static final Logger logger = LoggerFactory.getLogger(MerchantDocumentCommandServiceImpl.class);

    private final MerchantQueryRepository merchantQueryRepository;
    private final MerchantDocumentQueryRepository merchantDocumentQueryRepository;
    private final MerchantDocumentCommandRepository merchantDocumentCommandRepository;
    private final RedisService redisService;
    private final TracingMetrics tracingMetrics;

    @Inject
    public MerchantDocumentCommandServiceImpl(
            MerchantQueryRepository merchantQueryRepository,
            MerchantDocumentQueryRepository merchantDocumentQueryRepository,
            MerchantDocumentCommandRepository merchantDocumentCommandRepository,
            RedisService redisService,
            TracingMetrics tracingMetrics) {
        this.merchantQueryRepository = merchantQueryRepository;
        this.merchantDocumentQueryRepository = merchantDocumentQueryRepository;
        this.merchantDocumentCommandRepository = merchantDocumentCommandRepository;
        this.redisService = redisService;
        this.tracingMetrics = tracingMetrics;
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<MerchantDocumentResponse>> create(CreateMerchantDocumentRequest req) {
        Attributes attrs = Attributes.builder().put("merchant.id", req.getMerchantId()).build();
        logger.info("Creating merchant document | MerchantId: {}, Type: {}", req.getMerchantId(),
                req.getDocumentType());

        return tracingMetrics.traceAndMeasure("createMerchantDocument", "create", attrs, () -> {
            return merchantQueryRepository.findMerchantById(req.getMerchantId())
                    .chain(merchant -> {
                        if (merchant == null) {
                            logger.error("ResourceNotFound: Merchant not found with id {}", req.getMerchantId());
                            throw new ResourceNotFoundException("Merchant not found");
                        }

                        MerchantDocument doc = new MerchantDocument();
                        doc.setMerchantId(req.getMerchantId().intValue());
                        doc.setDocumentType(req.getDocumentType());
                        doc.setDocumentUrl(req.getDocumentUrl());
                        doc.setStatus("PENDING");

                        return merchantDocumentCommandRepository.persist(doc)
                                .chain(savedDoc -> {
                                    logger.info("Merchant document created successfully | Id: {}",
                                            savedDoc.getDocumentId());
                                    return Uni.createFrom()
                                            .item(ApiResponse.success("Merchant document created successfully",
                                                    MerchantDocumentResponse.from(savedDoc)));
                                });
                    });
        });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<MerchantDocumentResponse>> update(UpdateMerchantDocumentRequest req) {
        Attributes attrs = Attributes.builder().put("doc.id", req.getDocumentId()).build();
        logger.info("Updating merchant document | Id: {}", req.getDocumentId());

        return tracingMetrics.traceAndMeasure("updateMerchantDocument", "update", attrs, () -> {
            return merchantDocumentQueryRepository.findDocumentById(req.getDocumentId())
                    .chain(doc -> {
                        if (doc == null) {
                            logger.error("Merchant document not found with id {}", req.getDocumentId());
                            throw new ResourceNotFoundException("Merchant document not found");
                        }

                        return merchantQueryRepository.findMerchantById(req.getMerchantId())
                                .chain(merchant -> {
                                    if (merchant == null) {
                                        logger.error("Merchant not found with id {}", req.getMerchantId());
                                        throw new ResourceNotFoundException("Merchant not found");
                                    }

                                    doc.setMerchantId(req.getMerchantId().intValue());
                                    doc.setDocumentType(req.getDocumentType());
                                    doc.setDocumentUrl(req.getDocumentUrl());
                                    doc.setNote(req.getNote());
                                    doc.setStatus(req.getStatus());

                                    return merchantDocumentCommandRepository.persist(doc)
                                            .chain(savedDoc -> {
                                                String cacheKey = "merchant_doc:id:" + req.getDocumentId();

                                                return redisService.deleteReactive(cacheKey)
                                                        .map(v -> {
                                                            logger.info(
                                                                    "Merchant document updated successfully | Id: {}",
                                                                    req.getDocumentId());
                                                            return ApiResponse.success(
                                                                    "Merchant document updated successfully",
                                                                    MerchantDocumentResponse.from(savedDoc));
                                                        });
                                            });
                                });
                    });
        });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<MerchantDocumentResponse>> updateStatus(UpdateMerchantDocumentStatus req) {
        Attributes attrs = Attributes.builder().put("doc.id", req.getDocumentId()).build();
        logger.info("Updating merchant document status | Id: {}", req.getDocumentId());

        return tracingMetrics.traceAndMeasure("updateMerchantDocumentStatus", "update_status", attrs, () -> {
            return merchantDocumentQueryRepository.findDocumentById(req.getDocumentId())
                    .chain(doc -> {
                        if (doc == null) {
                            logger.error("Merchant document not found with id {}", req.getDocumentId());
                            throw new ResourceNotFoundException("Merchant document not found");
                        }

                        return merchantQueryRepository.findMerchantById(req.getMerchantId())
                                .chain(merchant -> {
                                    if (merchant == null) {
                                        logger.error("Merchant not found with id {}", req.getMerchantId());
                                        throw new ResourceNotFoundException("Merchant not found");
                                    }

                                    doc.setStatus(req.getStatus());
                                    doc.setNote(req.getNote());

                                    return merchantDocumentCommandRepository.persist(doc)
                                            .chain(savedDoc -> {
                                                String cacheKey = "merchant_doc:id:" + req.getDocumentId();

                                                return redisService.deleteReactive(cacheKey)
                                                        .map(v -> {
                                                            logger.info(
                                                                    "Merchant document status updated successfully | Id: {}",
                                                                    req.getDocumentId());
                                                            return ApiResponse.success(
                                                                    "Merchant document status updated successfully",
                                                                    MerchantDocumentResponse.from(savedDoc));
                                                        });
                                            });
                                });
                    });
        });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<MerchantDocumentResponseDeleteAt>> trash(Long id) {
        Attributes attrs = Attributes.builder().put("doc.id", id).build();
        logger.info("Trashing merchant document | Id: {}", id);

        return tracingMetrics.traceAndMeasure("trashMerchantDocument", "trash", attrs, () -> {
            return merchantDocumentCommandRepository.trashed(id)
                    .chain(doc -> {
                        if (doc == null) {
                            logger.error("Merchant document not found with id {}", id);
                            throw new ResourceNotFoundException("Merchant document not found");
                        }

                        String cacheKey = "merchant_doc:id:" + id;

                        return redisService.deleteReactive(cacheKey)
                                .map(v -> {
                                    logger.info("Merchant document trashed successfully | Id: {}", id);
                                    return ApiResponse.success("Merchant document trashed successfully",
                                            MerchantDocumentResponseDeleteAt.from(doc));
                                });
                    });
        });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<MerchantDocumentResponseDeleteAt>> restore(Long id) {
        Attributes attrs = Attributes.builder().put("doc.id", id).build();
        logger.info("Restoring merchant document | Id: {}", id);

        return tracingMetrics.traceAndMeasure("restoreMerchantDocument", "restore", attrs, () -> {
            return merchantDocumentCommandRepository.restore(id)
                    .chain(doc -> {
                        if (doc == null) {
                            logger.error("Merchant document restore failed - not found or must be trashed first with id {}", id);
                            throw new InvalidRequestException("Merchant document not found or must be trashed first");
                        }

                        String cacheKey = "merchant_doc:id:" + id;

                        return redisService.deleteReactive(cacheKey)
                                .map(v -> {
                                    logger.info("Merchant document restored successfully | Id: {}", id);
                                    return ApiResponse.success("Merchant document restored successfully",
                                            MerchantDocumentResponseDeleteAt.from(doc));
                                });
                    });
        });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<Boolean>> deletePermanent(Long id) {
        Attributes attrs = Attributes.builder().put("doc.id", id).build();
        logger.info("Permanently deleting merchant document | Id: {}", id);

        return tracingMetrics.traceAndMeasure("deletePermanentMerchantDocument", "delete_permanent", attrs, () -> {
            return merchantDocumentCommandRepository.deletePermanent(id)
                    .chain(success -> {
                        if (!success) {
                            logger.error("Permanent delete failed - merchant document not found or must be trashed before permanent deletion with id {}", id);
                            throw new InvalidRequestException("Merchant document not found or must be trashed before permanent deletion");
                        }

                        String cacheKey = "merchant_doc:id:" + id;

                        return redisService.deleteReactive(cacheKey)
                                .map(v -> {
                                    logger.info("Merchant document permanently deleted | Id: {}", id);
                                    return ApiResponse.success("Merchant document permanently deleted", true);
                                });
                    });
        });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<Boolean>> restoreAll() {
        logger.info("Restoring all merchant documents");

        return tracingMetrics.traceAndMeasure("restoreAllMerchantDocuments", "restore_all", () -> {
            return merchantDocumentCommandRepository.restoreAllDeleted()
                    .map(success -> {
                        if (!success) {
                            throw new ResourceNotFoundException("No trashed merchant documents found");
                        }
                        logger.info("Restored all merchant documents successfully");
                        return ApiResponse.success("All merchant documents restored successfully", true);
                    });
        });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<Boolean>> deleteAllPermanent() {
        logger.info("Permanently deleting all trashed merchant documents");

        return tracingMetrics.traceAndMeasure("deleteAllPermanentMerchantDocuments", "delete_all_permanent", () -> {
            return merchantDocumentCommandRepository.deleteAllDeleted()
                    .map(success -> {
                        if (!success) {
                            throw new ResourceNotFoundException("No trashed merchant documents found");
                        }
                        logger.info("All trashed merchant documents permanently deleted");
                        return ApiResponse.success("All trashed merchant documents permanently deleted", true);
                    });
        });
    }
}