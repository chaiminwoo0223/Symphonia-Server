package com.symphonia.auth.infrastructure.oauth.client;

import com.symphonia.auth.domain.client.SocialClient;
import com.symphonia.auth.domain.exception.InvalidSocialTokenException;
import com.symphonia.auth.domain.exception.SocialMemberInfoUnavailableException;
import com.symphonia.auth.domain.exception.SocialTokenVerificationUnavailableException;
import com.symphonia.auth.domain.identity.SocialIdentity;
import com.symphonia.auth.infrastructure.oauth.client.response.KakaoAccessTokenInfoResponse;
import com.symphonia.auth.infrastructure.oauth.client.response.KakaoUserInfoResponse;
import com.symphonia.auth.infrastructure.oauth.config.properties.KakaoOAuthProperties;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component("kakao")
@RequiredArgsConstructor
public class KakaoSocialClient implements SocialClient {
    private final RestClient restClient;
    private final KakaoOAuthProperties properties;

    @Override
    public SocialIdentity authenticate(String token) {
        verifyIssuedForThisApp(token);

        return fetchUserInfo(token).toSocialIdentity();
    }

    // 사용자 정보 조회는 다른 앱이 발급한 액세스 토큰도 통과시키므로, 토큰이 이 앱에서 발급됐는지 앱 ID로 확인한다.
    private void verifyIssuedForThisApp(String accessToken) {
        Optional.of(fetchAccessTokenInfo(accessToken))
                .filter(info -> info.isIssuedFor(properties.appId()))
                .orElseThrow(InvalidSocialTokenException::new);
    }

    private KakaoAccessTokenInfoResponse fetchAccessTokenInfo(String accessToken) {
        try {
            return restClient
                    .get()
                    .uri(properties.accessTokenInfoUri())
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .onStatus(
                            this::isInvalidTokenStatus,
                            (_, _) -> {
                                throw new InvalidSocialTokenException();
                            })
                    .requiredBody(KakaoAccessTokenInfoResponse.class);
        } catch (RestClientException e) {
            throw new SocialTokenVerificationUnavailableException();
        }
    }

    private boolean isInvalidTokenStatus(HttpStatusCode status) {
        return status.isSameCodeAs(HttpStatus.UNAUTHORIZED)
                || status.isSameCodeAs(HttpStatus.BAD_REQUEST);
    }

    private KakaoUserInfoResponse fetchUserInfo(String accessToken) {
        try {
            return restClient
                    .get()
                    .uri(properties.userInfoUri())
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .requiredBody(KakaoUserInfoResponse.class);
        } catch (RestClientException e) {
            throw new SocialMemberInfoUnavailableException();
        }
    }
}
