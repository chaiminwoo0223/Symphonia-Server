package com.symphonia.auth.infrastructure.oauth.client.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoAccessTokenInfoResponse(@JsonProperty("app_id") Long appId) {
    public boolean isIssuedFor(String expectedAppId) {
        return appId != null && appId.toString().equals(expectedAppId);
    }
}
