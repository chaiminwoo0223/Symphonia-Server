package com.symphonia.auth.infrastructure.oauth.client;

import com.symphonia.auth.domain.client.SocialClient;
import com.symphonia.auth.domain.exception.InvalidSocialTokenException;
import com.symphonia.auth.domain.exception.SocialMemberInfoFetchFailedException;
import com.symphonia.auth.domain.exception.SocialTokenVerificationFailedException;
import com.symphonia.auth.domain.identity.SocialIdentity;
import com.symphonia.auth.infrastructure.oauth.client.response.KakaoAccessTokenInfoResponse;
import com.symphonia.auth.infrastructure.oauth.client.response.KakaoUserInfoResponse;
import com.symphonia.auth.infrastructure.oauth.config.properties.KakaoOAuthProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
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
        KakaoAccessTokenInfoResponse response = fetchAccessTokenInfo(accessToken);

        if (response == null || response.appId() == null) {
            throw new SocialTokenVerificationFailedException();
        }
        if (!properties.appId().equals(String.valueOf(response.appId()))) {
            throw new InvalidSocialTokenException();
        }
    }

    private KakaoAccessTokenInfoResponse fetchAccessTokenInfo(String accessToken) {
        try {
            return restClient
                    .get()
                    .uri(properties.accessTokenInfoUri())
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .body(KakaoAccessTokenInfoResponse.class);
        } catch (HttpClientErrorException e) {
            // 만료되었거나 형식이 잘못된 토큰(401, 400)은 클라이언트 잘못이라 서버 오류(500)로 흘리지 않는다.
            // 호출 한도 초과(429)나 앱 권한 문제(403)는 토큰의 잘못이 아니다.
            if (isInvalidTokenStatus(e.getStatusCode())) {
                throw new InvalidSocialTokenException();
            }
            throw new SocialTokenVerificationFailedException();
        } catch (RestClientException e) {
            throw new SocialTokenVerificationFailedException();
        }
    }

    private boolean isInvalidTokenStatus(HttpStatusCode status) {
        return status.isSameCodeAs(HttpStatus.UNAUTHORIZED)
                || status.isSameCodeAs(HttpStatus.BAD_REQUEST);
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
