package com.symphonia.auth.infrastructure.oauth.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.symphonia.auth.domain.identity.SocialIdentity;

record KakaoUserInfoResponse(
        @JsonProperty("id") Long id, @JsonProperty("kakao_account") KakaoAccount kakaoAccount) {

    private static final String SOCIAL_PROVIDER = "KAKAO";

    record KakaoAccount(
            @JsonProperty("email") String email, @JsonProperty("profile") Profile profile) {

        record Profile(
                @JsonProperty("nickname") String nickname,
                @JsonProperty("profile_image_url") String profileImageUrl) {}
    }

    SocialIdentity toSocialIdentity() {
        KakaoAccount.Profile profile = kakaoAccount != null ? kakaoAccount.profile() : null;
        String email = kakaoAccount != null ? kakaoAccount.email() : null;
        String nickname = profile != null ? profile.nickname() : null;
        String profileImage = profile != null ? profile.profileImageUrl() : null;

        return new SocialIdentity(
                String.valueOf(id), nickname, email, profileImage, SOCIAL_PROVIDER);
    }
}
