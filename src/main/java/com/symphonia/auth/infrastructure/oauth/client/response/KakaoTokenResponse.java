package com.symphonia.auth.infrastructure.oauth.client.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoTokenResponse(@JsonProperty("access_token") String accessToken) {}
