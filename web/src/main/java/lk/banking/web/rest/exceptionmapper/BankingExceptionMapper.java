package lk.banking.web.rest.exceptionmapper;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import lk.banking.core.exception.*; // Import all your custom exceptions

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Maps custom BankingException types to appropriate HTTP error responses.
 */
@Provider // This annotation tells JAX-RS to discover and use this mapper
public class BankingExceptionMapper implements ExceptionMapper<BankingException> {

    private static final Logger LOGGER = Logger.getLogger(BankingExceptionMapper.class.getName());

    @Override
    public Response toResponse(BankingException exception) {
        LOGGER.log(Level.WARNING, "Mapping BankingException to HTTP response: " + exception.getMessage(), exception);

        if (exception instanceof AccountNotFoundException ||
                exception instanceof CustomerNotFoundException ||
                exception instanceof RoleNotFoundException ||
                exception instanceof UserNotFoundException ||
                exception instanceof ScheduledTransferException && exception.getMessage().contains("not found")) { // Specific check for ST not found

            return Response.status(Response.Status.NOT_FOUND) // 404
                    .entity(new ErrorResponse(404, exception.getMessage()))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } else if (exception instanceof InsufficientFundsException ||
                exception instanceof InvalidTransactionException ||
                exception instanceof ValidationException) {

            return Response.status(Response.Status.BAD_REQUEST) // 400
                    .entity(new ErrorResponse(400, exception.getMessage()))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } else if (exception instanceof ResourceConflictException) {
            return Response.status(Response.Status.CONFLICT) // 409
                    .entity(new ErrorResponse(409, exception.getMessage()))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } else if (exception instanceof UnauthorizedAccessException) {
            return Response.status(Response.Status.UNAUTHORIZED) // 401
                    .entity(new ErrorResponse(401, exception.getMessage()))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } else if (exception instanceof TransactionTimeoutException) {
            return Response.status(Response.Status.REQUEST_TIMEOUT) // 408
                    .entity(new ErrorResponse(408, exception.getMessage()))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } else if (exception instanceof AccountLockedException) {
            return Response.status(Response.Status.FORBIDDEN) // 403 - User is identified, but access is denied due to account state
                    .entity(new ErrorResponse(403, exception.getMessage()))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
        // Default for any BankingException not specifically caught above
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR) // 500
                .entity(new ErrorResponse(500, "An unexpected banking error occurred: " + exception.getMessage()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    // --- Helper class for consistent error response format ---
    public static class ErrorResponse {
        public int status;
        public String message;
        public ErrorResponse(int status, String message) {
            this.status = status;
            this.message = message;
        }
    }
}