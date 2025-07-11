// File: lk/banking/web/rest/AuthResource.java
package lk.banking.web.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lk.banking.core.dto.LoginRequestDto;    // DTO for login request
import lk.banking.core.dto.UserDto;           // DTO for user response (without password)
import lk.banking.core.entity.User;
import lk.banking.core.mapper.UserMapper;       // Mapper to convert User entity to UserDto
import lk.banking.security.AuthenticationService; // Your authentication EJB service

/**
 * JAX-RS Resource for user authentication.
 * Handles login and potentially other authentication-related operations.
 */
@Path("/auth") // This path will be accessed as /api/auth (due to ApplicationConfig)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    private AuthenticationService authenticationService;

    /**
     * Handles user login requests.
     * Takes a LoginRequestDto (username, password) and returns a UserDto on success.
     *
     * @param loginRequestDto Contains username and plain-text password.
     * @return 200 OK with UserDto if authentication is successful.
     * @throws lk.banking.core.exception.UnauthorizedAccessException if credentials are invalid.
     *      (This exception will be mapped to 401 Unauthorized by BankingExceptionMapper).
     */
    @POST
    @Path("/login") // Full path: /api/auth/login
    public Response login(LoginRequestDto loginRequestDto) {
        // Authenticate the user using the AuthenticationService EJB
        // authenticationService.authenticate will throw UnauthorizedAccessException on failure
        User authenticatedUser = authenticationService.authenticate(
                loginRequestDto.getUsername(),
                loginRequestDto.getPassword()
        );

        // If authentication is successful, convert the User entity to UserDto (to exclude sensitive info like hashed password)
        UserDto userDto = UserMapper.toDto(authenticatedUser);

        // Return 200 OK with the UserDto
        return Response.ok(userDto).build();
    }

    // You might add other endpoints here later, such as:
    // @POST @Path("/logout")
    // @POST @Path("/refresh-token")
    // @POST @Path("/change-password") (though changePassword is in AuthenticationService, you might expose it differently here)
    // @POST @Path("/forgot-password")
}