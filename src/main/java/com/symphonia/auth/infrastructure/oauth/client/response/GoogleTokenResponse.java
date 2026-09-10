package com.symphonia.auth.infrastructure.oauth.client.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GoogleTokenResponse(@JsonProperty("id_token") String idToken) {}
