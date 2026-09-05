package com.sanedge.gateway.exception;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import java.lang.reflect.Method;
import io.quarkus.logging.Log;
import com.sanedge.common.exception.*;

@GraphQLErrorHandler
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class GraphQLErrorHandlerInterceptor {

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        Object result = context.proceed();
        if (result instanceof Uni) {
            return ((Uni<?>) result).map(this::checkAndMapPayload);
        }
        return checkAndMapPayload(result);
    }

    private Object checkAndMapPayload(Object entity) {
        if (entity == null) {
            return null;
        }

        try {
            Method statusMethod = null;
            try {
                statusMethod = entity.getClass().getMethod("status");
            } catch (NoSuchMethodException e) {
                try {
                    statusMethod = entity.getClass().getMethod("getStatus");
                } catch (NoSuchMethodException ex) {
                    // No status method found
                }
            }

            if (statusMethod != null) {
                String status = (String) statusMethod.invoke(entity);
                if ("failed".equalsIgnoreCase(status) || "error".equalsIgnoreCase(status)) {
                    String message = "Operation failed";
                    Method messageMethod = null;
                    try {
                        messageMethod = entity.getClass().getMethod("message");
                    } catch (NoSuchMethodException e) {
                        try {
                            messageMethod = entity.getClass().getMethod("getMessage");
                        } catch (NoSuchMethodException ex) {
                            // No message method found
                        }
                    }

                    if (messageMethod != null) {
                        message = (String) messageMethod.invoke(entity);
                    }

                    Log.warnf("Payload indicated failure: status=%s, message='%s'. Mapping to custom exception.", status, message);
                    throw mapToException(message);
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            Log.error("Error occurred while processing GraphQL response error handling interceptor", e);
        }
        return entity;
    }

    private RuntimeException mapToException(String message) {
        if (message == null) {
            return new InvalidRequestException("Operation failed");
        }
        String msgLower = message.toLowerCase();
        if (msgLower.contains("not found")) {
            return new ResourceNotFoundException(message);
        } else if (msgLower.contains("already exists") || msgLower.contains("already registered")) {
            return new ResourceAlreadyExistsException(message);
        } else if (msgLower.contains("unauthorized") || msgLower.contains("credentials") || msgLower.contains("expired") || msgLower.contains("wrong password") || msgLower.contains("invalid credential")) {
            return new UnauthorizedException(message);
        } else if (msgLower.contains("forbidden") || msgLower.contains("denied")) {
            return new ForbiddenException(message);
        } else {
            return new InvalidRequestException(message);
        }
    }
}
