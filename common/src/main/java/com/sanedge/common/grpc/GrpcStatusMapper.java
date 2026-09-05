package com.sanedge.common.grpc;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import com.sanedge.common.exception.ForbiddenException;
import com.sanedge.common.exception.InvalidRequestException;
import com.sanedge.common.exception.ResourceAlreadyExistsException;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.common.exception.UnauthorizedException;

public final class GrpcStatusMapper {
    private GrpcStatusMapper() {
    }

    public static StatusRuntimeException toStatusRuntimeException(Throwable failure) {
        Throwable cause = unwrap(failure);
        if (cause instanceof StatusRuntimeException grpcFailure) {
            return grpcFailure;
        }

        Status status = switch (cause) {
            case ResourceNotFoundException ignored -> Status.NOT_FOUND;
            case ResourceAlreadyExistsException ignored -> Status.ALREADY_EXISTS;
            case InvalidRequestException ignored -> Status.INVALID_ARGUMENT;
            case UnauthorizedException ignored -> Status.UNAUTHENTICATED;
            case ForbiddenException ignored -> Status.PERMISSION_DENIED;
            case IllegalArgumentException ignored -> Status.INVALID_ARGUMENT;
            case IllegalStateException ignored -> Status.FAILED_PRECONDITION;
            default -> Status.INTERNAL;
        };

        String message = cause.getMessage();
        return status.withDescription(message == null || message.isBlank() ? status.getCode().name() : message)
                .asRuntimeException();
    }

    private static Throwable unwrap(Throwable failure) {
        Throwable current = failure;
        while ((current instanceof CompletionException || current instanceof ExecutionException)
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }
}
