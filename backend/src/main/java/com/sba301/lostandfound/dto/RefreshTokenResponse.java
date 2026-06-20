package com.sba301.lostandfound.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RefreshTokenResponse {

    private final String accessToken;

    @Builder.Default
    private final String tokenType = "Bearer";
}
