package com.sanedge.card.service;

import java.util.List;

import com.sanedge.card.domain.requests.PostPaymentRequest;
import com.sanedge.card.domain.response.CardPaymentResponse;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;

import io.smallrye.mutiny.Uni;

public interface CardPaymentService {
    Uni<ApiResponse<CardPaymentResponse>> postPayment(PostPaymentRequest req);
    Uni<ApiResponsePagination<List<CardPaymentResponse>>> getPaymentHistory(String cardNumber, int page, int size);
    Uni<ApiResponse<Long>> countPayments(String cardNumber);
}
