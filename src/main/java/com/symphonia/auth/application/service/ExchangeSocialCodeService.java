package com.symphonia.auth.application.service;

import com.symphonia.auth.application.dto.result.OAuthMemberResult;
import com.symphonia.auth.application.usecase.ExchangeSocialCodeUseCase;
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
public class ExchangeSocialCodeService implements ExchangeSocialCodeUseCase {
    private final Map<String, SocialClient> socialClients;

    @Override
    public OAuthMemberResult exchange(String provider, String code) {
        SocialClient socialClient = resolveSocialClient(provider);
        SocialIdentity identity = socialClient.authenticate(code);

        return OAuthMemberResult.from(identity);
    }

    private SocialClient resolveSocialClient(String provider) {
        return Optional.ofNullable(socialClients.get(provider.toLowerCase(Locale.ROOT)))
                .orElseThrow(UnsupportedSocialProviderException::new);
    }
}
