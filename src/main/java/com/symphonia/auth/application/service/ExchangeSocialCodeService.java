package com.symphonia.auth.application.service;

import com.symphonia.auth.application.dto.result.OAuthMemberResult;
import com.symphonia.auth.application.usecase.ExchangeSocialCodeUseCase;
import com.symphonia.auth.domain.client.SocialClient;
import com.symphonia.auth.domain.exception.UnsupportedSocialProviderException;
import com.symphonia.auth.domain.identity.SocialIdentity;
import com.symphonia.common.annotation.CommandService;
import com.symphonia.member.domain.entity.SocialProvider;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@CommandService
@RequiredArgsConstructor
public class ExchangeSocialCodeService implements ExchangeSocialCodeUseCase {
    private final Map<String, SocialClient> socialClients;

    @Override
    public OAuthMemberResult exchange(String provider, String code) {
        SocialClient socialClient = resolveSocialClient(provider);
        SocialIdentity identity = socialClient.authenticate(code);

        return new OAuthMemberResult(
                identity.socialId(),
                identity.nickname(),
                identity.email(),
                identity.profileImage(),
                SocialProvider.valueOf(identity.socialProvider()));
    }

    private SocialClient resolveSocialClient(String provider) {
        SocialClient socialClient = socialClients.get(provider.toLowerCase(Locale.ROOT));

        if (socialClient == null) {
            throw new UnsupportedSocialProviderException();
        }
        return socialClient;
    }
}
