package com.symphonia.auth.domain.provider;

public interface AccessTokenProvider {
    String generate(String memberId, String role);

    String getMemberId(String accessToken);

    String getRole(String accessToken);

    long getRemainingTime(String accessToken);

    boolean validate(String accessToken);
}
