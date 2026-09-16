package com.symphonia.auth.infrastructure.oauth.client;

import com.symphonia.auth.domain.client.SocialClient;
import com.symphonia.auth.domain.exception.SocialAuthenticationFailedException;
import com.symphonia.auth.domain.exception.SocialMemberInfoFetchFailedException;
import com.symphonia.auth.domain.identity.SocialIdentity;
import com.symphonia.auth.infrastructure.oauth.client.response.KakaoTokenResponse;
import com.symphonia.auth.infrastructure.oauth.client.response.KakaoUserInfoResponse;
import com.symphonia.auth.infrastructure.oauth.config.properties.KakaoOAuthProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component("kakao")
@RequiredArgsConstructor
public class KakaoSocialClient implements SocialClient {
    private final RestClient restClient;
    private final KakaoOAuthProperties properties;

    @Override
    public SocialIdentity authenticate(String code) {
        String accessToken = exchangeAccessToken(code);

        return fetchUserInfo(accessToken).toSocialIdentity();
    }

    private String exchangeAccessToken(String code) {
        KakaoTokenResponse response =
                AuthorizationCodeClient.exchange(
                        restClient,
                        properties.tokenUri(),
                        properties.clientId(),
                        properties.clientSecret(),
                        properties.redirectUri(),
                        code,
                        KakaoTokenResponse.class);

        if (response == null || response.accessToken() == null) {
            throw new SocialAuthenticationFailedException();
        }
        return response.accessToken();
    }

    private KakaoUserInfoResponse fetchUserInfo(String accessToken) {
        try {
            KakaoUserInfoResponse response =
                    restClient
                            .get()
                            .uri(properties.userInfoUri())
                            .headers(headers -> headers.setBearerAuth(accessToken))
                            .retrieve()
                            .body(KakaoUserInfoResponse.class);

            if (response == null) {
                throw new SocialMemberInfoFetchFailedException();
            }
            return response;
        } catch (RestClientException e) {
            throw new SocialMemberInfoFetchFailedException();
        }
    }
}
