package com.sanedge.common.adapter.merchant;

import com.sanedge.common.adapter.model.Merchant;
import com.sanedge.common.adapter.support.ProtoTime;
import com.sanedge.common.exception.ResourceNotFoundException;

import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import pb.merchant.Merchant.FindByApiKeyRequest;
import pb.merchant.Merchant.FindByIdMerchantRequest;
import pb.merchant.Merchant.MerchantResponse;
import pb.merchant.MerchantQueryService;

@ApplicationScoped
public class MerchantAdapter implements MerchantPort {

    @GrpcClient("merchant")
    MerchantQueryService query;

    @Override
    public Uni<Merchant> findByApiKey(String apiKey) {
        return query.findByApiKey(FindByApiKeyRequest.newBuilder().setApiKey(apiKey).build())
                .map(resp -> {
                    if (resp == null || !resp.hasData()) {
                        throw new ResourceNotFoundException("Merchant not found for api key");
                    }
                    return toMerchant(resp.getData());
                });
    }

    @Override
    public Uni<Merchant> findByMerchantId(int merchantId) {
        return query.findByIdMerchant(FindByIdMerchantRequest.newBuilder().setMerchantId(merchantId).build())
                .map(resp -> {
                    if (resp == null || !resp.hasData()) {
                        throw new ResourceNotFoundException("Merchant not found: " + merchantId);
                    }
                    return toMerchant(resp.getData());
                });
    }

    private static Merchant toMerchant(MerchantResponse m) {
        if (m == null) {
            return null;
        }
        return new Merchant(m.getId(), m.getName(), m.getApiKey(), m.getStatus(), m.getUserId(),
                ProtoTime.parse(m.getCreatedAt()), ProtoTime.parse(m.getUpdatedAt()));
    }
}
