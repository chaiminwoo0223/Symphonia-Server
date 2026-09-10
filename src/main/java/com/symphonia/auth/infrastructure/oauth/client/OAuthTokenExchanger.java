package com.symphonia.auth.infrastructure.oauth.client;

import com.symphonia.auth.domain.exception.InvalidAuthorizationCodeException;
import com.symphonia.auth.domain.exception.SocialAuthenticationFailedException;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

final class OAuthTokenExchanger {
    private static final String AUTHORIZATION_CODE_GRANT_TYPE = "authorization_code";

    private OAuthTokenExchanger() {}

    public static <T> T exchange(
            RestClient restClient,
            String tokenUri,
            String clientId,
            String clientSecret,
            String redirectUri,
            String code,
            Class<T> responseType) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", AUTHORIZATION_CODE_GRANT_TYPE);
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("redirect_uri", redirectUri);
        body.add("code", code);

        try {
            return restClient
                    .post()
                    .uri(tokenUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(responseType);
        } catch (HttpClientErrorException e) {
            // 만료·재사용된 인가 코드 등 클라이언트 잘못(4xx)을 서버 오류(500)로 흘리지 않는다.
            throw new InvalidAuthorizationCodeException();
        } catch (RestClientException e) {
            throw new SocialAuthenticationFailedException();
        }
    }
}
