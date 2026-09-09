package com.symphonia.auth.infrastructure.oauth.client;

import com.fasterxml.jackson.annotation.JsonProperty;

record GoogleTokenResponse(@JsonProperty("id_token") String idToken) {}
