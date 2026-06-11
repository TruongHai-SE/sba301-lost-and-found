package com.sba301.lostandfound.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {

    private final String accessToken;

    @Builder.Default
    private final String tokenType = "Bearer";

    private final Long userId;
    private final String name;
    private final String mail;
    private final String userType;

    @JsonIgnore
    private final String refreshToken;
}
