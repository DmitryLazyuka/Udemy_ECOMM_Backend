package org.example.udemyproject.service;

import jakarta.validation.Valid;
import org.example.udemyproject.payload.AuthenticationResult;
import org.example.udemyproject.payload.UserResponse;
import org.example.udemyproject.security.request.LoginRequest;
import org.example.udemyproject.security.request.SignupRequest;
import org.example.udemyproject.security.response.MessageResponse;
import org.example.udemyproject.security.response.UserInfoResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

public interface AuthService {


    AuthenticationResult login(LoginRequest loginRequest);

    ResponseEntity<MessageResponse> register(@Valid SignupRequest signupRequest);

    UserInfoResponse getCurrentUserDetails(Authentication authentication);

    ResponseCookie logoutUser();

    UserResponse getAllSellers(Pageable pageDetails);
}
