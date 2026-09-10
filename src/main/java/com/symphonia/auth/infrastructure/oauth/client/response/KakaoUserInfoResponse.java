package com.symphonia.auth.infrastructure.oauth.client.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.symphonia.auth.domain.identity.SocialIdentity;
import com.symphonia.member.domain.entity.SocialProvider;

public record KakaoUserInfoResponse(
        @JsonProperty("id") Long id, @JsonProperty("kakao_account") KakaoAccount kakaoAccount) {
    public record KakaoAccount(
            @JsonProperty("email") String email, @JsonProperty("profile") Profile profile) {
        public record Profile(
                @JsonProperty("nickname") String nickname,
                @JsonProperty("profile_image_url") String profileImageUrl) {}
    }

    public SocialIdentity toSocialIdentity() {
        KakaoAccount.Profile profile = kakaoAccount != null ? kakaoAccount.profile() : null;
        String email = kakaoAccount != null ? kakaoAccount.email() : null;
        String nickname = profile != null ? profile.nickname() : null;
        String profileImage = profile != null ? profile.profileImageUrl() : null;

        return new SocialIdentity(
                String.valueOf(id), nickname, email, profileImage, SocialProvider.KAKAO.name());
    }
}
