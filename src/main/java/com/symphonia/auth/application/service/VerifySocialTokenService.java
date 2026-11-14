package com.symphonia.auth.application.service;

import com.symphonia.auth.application.dto.result.OAuthMemberResult;
import com.symphonia.auth.application.usecase.VerifySocialTokenUseCase;
import com.symphonia.auth.domain.client.SocialClient;
import com.symphonia.auth.domain.exception.UnsupportedSocialProviderException;
import com.symphonia.auth.domain.identity.SocialIdentity;
import com.symphonia.common.annotation.CommandService;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@CommandService
@RequiredArgsConstructor
public class VerifySocialTokenService implements VerifySocialTokenUseCase {
    private final Map<String, SocialClient> socialClients;

    @Override
    public OAuthMemberResult verify(String provider, String token) {
        SocialClient socialClient = resolveSocialClient(provider);
        SocialIdentity identity = socialClient.authenticate(token);

        return OAuthMemberResult.from(identity);
    }

    private SocialClient resolveSocialClient(String provider) {
        return Optional.ofNullable(socialClients.get(provider.toLowerCase(Locale.ROOT)))
                .orElseThrow(UnsupportedSocialProviderException::new);
    }
}
