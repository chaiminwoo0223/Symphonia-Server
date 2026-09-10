package com.symphonia.auth.infrastructure.oauth.client;

import com.symphonia.auth.domain.client.SocialClient;
import com.symphonia.auth.domain.exception.InvalidAuthorizationCodeException;
import com.symphonia.auth.domain.exception.SocialAuthenticationFailedException;
import com.symphonia.auth.domain.exception.SocialMemberInfoFetchFailedException;
import com.symphonia.auth.domain.identity.SocialIdentity;
import com.symphonia.auth.infrastructure.oauth.config.properties.KakaoOAuthProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component("kakao")
@RequiredArgsConstructor
public class KakaoSocialClient implements SocialClient {
    private static final String GRANT_TYPE = "authorization_code";
    private static final String BEARER_PREFIX = "Bearer ";

    private final RestClient restClient;
    private final KakaoOAuthProperties properties;

    @Override
    public SocialIdentity authenticate(String code) {
        String accessToken = exchangeAccessToken(code);

        return fetchUserInfo(accessToken).toSocialIdentity();
    }

    private String exchangeAccessToken(String code) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", GRANT_TYPE);
        body.add("client_id", properties.clientId());
        body.add("client_secret", properties.clientSecret());
        body.add("redirect_uri", properties.redirectUri());
        body.add("code", code);

        try {
            KakaoTokenResponse response =
                    restClient
                            .post()
                            .uri(properties.tokenUri())
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .body(body)
                            .retrieve()
                            .body(KakaoTokenResponse.class);

            if (response == null || response.accessToken() == null) {
                throw new SocialAuthenticationFailedException();
            }
            return response.accessToken();
        } catch (HttpClientErrorException e) {
            // 만료·재사용된 인가 코드 등 클라이언트 잘못(4xx)을 서버 오류(500)로 흘리지 않는다.
            throw new InvalidAuthorizationCodeException();
        } catch (RestClientException e) {
            throw new SocialAuthenticationFailedException();
        }
    }

    private KakaoUserInfoResponse fetchUserInfo(String accessToken) {
        try {
            KakaoUserInfoResponse response =
                    restClient
                            .get()
                            .uri(properties.userInfoUri())
                            .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + accessToken)
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
