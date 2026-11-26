package com.symphonia.auth.infrastructure.oauth.client;

import com.symphonia.auth.domain.client.SocialClient;
import com.symphonia.auth.domain.exception.InvalidSocialTokenException;
import com.symphonia.auth.domain.exception.SocialTokenVerificationUnavailableException;
import com.symphonia.auth.domain.identity.SocialIdentity;
import com.symphonia.member.domain.entity.SocialProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

@Component("google")
@RequiredArgsConstructor
public class GoogleSocialClient implements SocialClient {
    private final JwtDecoder googleIdTokenDecoder;

    @Override
    public SocialIdentity authenticate(String token) {
        return toSocialIdentity(decode(token));
    }

    private Jwt decode(String idToken) {
        try {
            return googleIdTokenDecoder.decode(idToken);
        } catch (BadJwtException e) {
            throw new InvalidSocialTokenException();
        } catch (JwtException e) {
            throw new SocialTokenVerificationUnavailableException();
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
