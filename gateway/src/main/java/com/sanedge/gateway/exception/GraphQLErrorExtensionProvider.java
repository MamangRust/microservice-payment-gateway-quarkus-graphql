package com.sanedge.gateway.exception;

import io.grpc.StatusRuntimeException;
import io.smallrye.graphql.api.ErrorExtensionProvider;
import jakarta.json.Json;
import jakarta.json.JsonValue;
import com.sanedge.common.exception.*;

public class GraphQLErrorExtensionProvider implements ErrorExtensionProvider {

    @Override
    public String getKey() {
        return "code";
    }

    @Override
    public JsonValue mapValueFrom(Throwable exception) {
        String code = determineErrorCode(exception);
        return Json.createValue(code);
    }

    private String determineErrorCode(Throwable exception) {
        if (exception instanceof ResourceNotFoundException) {
            return "NOT_FOUND";
        } else if (exception instanceof ResourceAlreadyExistsException) {
            return "CONFLICT";
        } else if (exception instanceof UnauthorizedException) {
            return "UNAUTHORIZED";
        } else if (exception instanceof ForbiddenException) {
            return "FORBIDDEN";
        } else if (exception instanceof InvalidRequestException) {
            return "BAD_REQUEST";
        } else if (exception instanceof StatusRuntimeException) {
            StatusRuntimeException statusException = (StatusRuntimeException) exception;
            return switch (statusException.getStatus().getCode()) {
                case NOT_FOUND -> "NOT_FOUND";
                case ALREADY_EXISTS -> "CONFLICT";
                case INVALID_ARGUMENT -> "BAD_REQUEST";
                case FAILED_PRECONDITION -> "BAD_REQUEST";
                case PERMISSION_DENIED -> "FORBIDDEN";
                case UNAUTHENTICATED -> "UNAUTHORIZED";
                case UNAVAILABLE -> "SERVICE_UNAVAILABLE";
                case DEADLINE_EXCEEDED -> "TIMEOUT";
                default -> "INTERNAL_SERVER_ERROR";
            };
        } else if (exception instanceof jakarta.validation.ConstraintViolationException) {
            return "BAD_REQUEST";
        }
        return "INTERNAL_SERVER_ERROR";
    }
}
