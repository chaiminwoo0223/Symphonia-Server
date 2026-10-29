package com.symphonia.auth.domain.provider;

public interface RefreshTokenProvider {
    String generate();

    long getExpirationTime();
}
