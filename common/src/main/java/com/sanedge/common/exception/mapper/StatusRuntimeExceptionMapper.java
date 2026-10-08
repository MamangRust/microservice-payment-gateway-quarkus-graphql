import com.sanedge.common.domain.response.ApiResponse;

import io.grpc.StatusRuntimeException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class StatusRuntimeExceptionMapper implements ExceptionMapper<StatusRuntimeException> {

  @Override
  public Response toResponse(StatusRuntimeException ex) {
    int httpStatus = switch (ex.getStatus().getCode()) {
      case NOT_FOUND -> 404;
      case ALREADY_EXISTS -> 409;
      case INVALID_ARGUMENT -> 400;
      case FAILED_PRECONDITION -> 422;
      case PERMISSION_DENIED -> 403;
      case UNAUTHENTICATED -> 401;
      case RESOURCE_EXHAUSTED -> 429;
      case UNAVAILABLE -> 503;
      case DEADLINE_EXCEEDED -> 504;
      default -> 500;
    };

    String message = ex.getStatus().getDescription();
    if (message == null || message.isBlank()) {
      message = ex.getStatus().getCode().name();
    }

    ApiResponse<Void> body = ApiResponse.error(message);

    return Response.status(httpStatus)
        .type(MediaType.APPLICATION_JSON)
        .entity(body)
        .build();
  }
}
