package org.example.udemyproject.security.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class UserInfoResponse {
    private Long id;
    private String username;
    private String jwt;
    private List<String> roles;

    public UserInfoResponse(Long id, String username, String jwt, List<String> roles) {
        this.id = id;
        this.username = username;
        this.jwt = jwt;
        this.roles = roles;
    }
}
