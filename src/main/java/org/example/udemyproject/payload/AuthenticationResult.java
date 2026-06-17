package org.example.udemyproject.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.example.udemyproject.security.response.UserInfoResponse;
import org.springframework.http.ResponseCookie;

@Data
@AllArgsConstructor
public class AuthenticationResult {
    private final UserInfoResponse userInfoResponse;
    private final ResponseCookie jwtCookie;
}
