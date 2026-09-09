package com.symphonia.auth.infrastructure.oauth.client;

import com.fasterxml.jackson.annotation.JsonProperty;

record KakaoTokenResponse(@JsonProperty("access_token") String accessToken) {}
