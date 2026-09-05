package com.sanedge.gateway.exception;

import io.smallrye.graphql.api.ErrorExtensionProvider;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

public class ValidationErrorExtensionProvider implements ErrorExtensionProvider {

    @Override
    public String getKey() {
        return "errors";
    }

    @Override
    public JsonValue mapValueFrom(Throwable exception) {
        if (exception instanceof ConstraintViolationException) {
            ConstraintViolationException cve = (ConstraintViolationException) exception;
            JsonObjectBuilder builder = Json.createObjectBuilder();
            for (ConstraintViolation<?> violation : cve.getConstraintViolations()) {
                String propertyPath = violation.getPropertyPath().toString();
                String field = propertyPath;
                int lastDot = propertyPath.lastIndexOf('.');
                if (lastDot != -1) {
                    field = propertyPath.substring(lastDot + 1);
                }
                builder.add(field, violation.getMessage());
            }
            return builder.build();
        }
        return null;
    }
}
