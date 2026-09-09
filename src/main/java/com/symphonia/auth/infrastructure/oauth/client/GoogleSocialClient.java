package com.symphonia.auth.infrastructure.oauth.client;

import com.symphonia.auth.domain.client.SocialClient;
import com.symphonia.auth.domain.exception.InvalidAuthorizationCodeException;
import com.symphonia.auth.domain.exception.SocialAuthenticationFailedException;
import com.symphonia.auth.domain.exception.SocialMemberInfoFetchFailedException;
import com.symphonia.auth.domain.identity.SocialIdentity;
import com.symphonia.auth.infrastructure.oauth.config.properties.GoogleOAuthProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component("google")
@RequiredArgsConstructor
public class GoogleSocialClient implements SocialClient {

    private static final String SOCIAL_PROVIDER = "GOOGLE";
    private static final String GRANT_TYPE = "authorization_code";

    private final RestClient restClient;
    private final JwtDecoder googleIdTokenDecoder;
    private final GoogleOAuthProperties properties;

    @Override
    public SocialIdentity authenticate(String code) {
        String idToken = exchangeIdToken(code);

        return toSocialIdentity(decode(idToken));
    }

    private String exchangeIdToken(String code) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", GRANT_TYPE);
        body.add("client_id", properties.clientId());
        body.add("client_secret", properties.clientSecret());
        body.add("redirect_uri", properties.redirectUri());
        body.add("code", code);

        try {
            GoogleTokenResponse response =
                    restClient
                            .post()
                            .uri(properties.tokenUri())
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .body(body)
                            .retrieve()
                            .body(GoogleTokenResponse.class);

            if (response == null || response.idToken() == null) {
                throw new SocialAuthenticationFailedException();
            }
            return response.idToken();
        } catch (HttpClientErrorException e) {
            // 만료·재사용된 인가 코드 등 클라이언트 잘못(4xx)을 서버 오류(500)로 흘리지 않는다.
            throw new InvalidAuthorizationCodeException();
        } catch (RestClientException e) {
            throw new SocialAuthenticationFailedException();
        }
    }

    private Jwt decode(String idToken) {
        try {
            return googleIdTokenDecoder.decode(idToken);
        } catch (JwtException e) {
            throw new SocialMemberInfoFetchFailedException();
        }
    }

    private SocialIdentity toSocialIdentity(Jwt jwt) {
        return new SocialIdentity(
                jwt.getSubject(),
                jwt.getClaimAsString("name"),
                jwt.getClaimAsString("email"),
                jwt.getClaimAsString("picture"),
                SOCIAL_PROVIDER);
    }
}
