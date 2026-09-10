package com.symphonia.auth.infrastructure.oauth.client;

import com.symphonia.auth.domain.client.SocialClient;
import com.symphonia.auth.domain.exception.SocialAuthenticationFailedException;
import com.symphonia.auth.domain.exception.SocialMemberInfoFetchFailedException;
import com.symphonia.auth.domain.identity.SocialIdentity;
import com.symphonia.auth.infrastructure.oauth.client.response.GoogleTokenResponse;
import com.symphonia.auth.infrastructure.oauth.config.properties.GoogleOAuthProperties;
import com.symphonia.member.domain.entity.SocialProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component("google")
@RequiredArgsConstructor
public class GoogleSocialClient implements SocialClient {
    private final RestClient restClient;
    private final JwtDecoder googleIdTokenDecoder;
    private final GoogleOAuthProperties properties;

    @Override
    public SocialIdentity authenticate(String code) {
        String idToken = exchangeIdToken(code);

        return toSocialIdentity(decode(idToken));
    }

    private String exchangeIdToken(String code) {
        GoogleTokenResponse response =
                AuthorizationCodeClient.exchange(
                        restClient,
                        properties.tokenUri(),
                        properties.clientId(),
                        properties.clientSecret(),
                        properties.redirectUri(),
                        code,
                        GoogleTokenResponse.class);

        if (response == null || response.idToken() == null) {
            throw new SocialAuthenticationFailedException();
        }
        return response.idToken();
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
                SocialProvider.GOOGLE.name());
    }
}
