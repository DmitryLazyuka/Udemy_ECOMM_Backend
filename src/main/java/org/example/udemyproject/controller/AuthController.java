package org.example.udemyproject.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.example.udemyproject.config.AppConstants;
import org.example.udemyproject.payload.AuthenticationResult;
import org.example.udemyproject.security.request.LoginRequest;
import org.example.udemyproject.security.request.SignupRequest;
import org.example.udemyproject.security.response.MessageResponse;
import org.example.udemyproject.security.response.UserInfoResponse;
import org.example.udemyproject.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication and current-user endpoints")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/signin")
    @Operation(summary = "Sign in", description = "Authenticates a user and returns the current user information with a JWT cookie.")
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User authenticated successfully",
                    content = @Content(schema = @Schema(implementation = UserInfoResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        AuthenticationResult result = authService.login(loginRequest);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, result.getJwtCookie().toString())
                .body(result.getUserInfoResponse());
    }

    @PostMapping("/signup")
    @Operation(summary = "Sign up", description = "Registers a new user account.")
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User registered successfully",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid or duplicate registration data",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signupRequest) {
        return authService.register(signupRequest);

    }

    @GetMapping("/username")
    @Operation(summary = "Get current username", description = "Returns the username of the authenticated user.")
    @SecurityRequirement(name = "bearer")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Username returned successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public String currentUserName(Authentication authentication) {
        if (authentication != null) {
            return authentication.getName();
        }
        return "";
    }

    @GetMapping("/user")
    @Operation(summary = "Get current user", description = "Returns details for the authenticated user.")
    @SecurityRequirement(name = "bearer")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User details returned successfully",
                    content = @Content(schema = @Schema(implementation = UserInfoResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<UserInfoResponse> getUserDetails(Authentication authentication) {
        return ResponseEntity.ok().body(authService.getCurrentUserDetails(authentication));
    }

    @PostMapping("/signout")
    @Operation(summary = "Sign out", description = "Clears the JWT cookie for the authenticated user.")
    @SecurityRequirement(name = "bearer")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User signed out successfully",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<?> signoutUser() {
        ResponseCookie jwtCookie = authService.logoutUser();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .body(new MessageResponse("Successfully logged out"));
    }

    @GetMapping("/sellers")
    public ResponseEntity<?> getAllSellers(
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber

    ) {
         Sort sortByAndOrder = Sort.by(AppConstants.SORT_USERS_BY).descending();
         Pageable pageDetails = PageRequest.of(pageNumber, Integer.parseInt(AppConstants.PAGE_SIZE), sortByAndOrder);
         return  ResponseEntity.ok(authService.getAllSellers(pageDetails));

    }
}
