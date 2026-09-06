package com.symphonia.auth.application.service;

import static org.mockito.Mockito.verify;

import com.symphonia.UnitTest;
import com.symphonia.auth.domain.repository.RefreshTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("ForceRevokeService 단위 테스트")
class ForceRevokeServiceTest extends UnitTest {

    @InjectMocks private ForceRevokeService forceRevokeService;

    @Mock private RefreshTokenRepository refreshTokenRepository;

    private static final String MEMBER_ID = "1";

    @Nested
    @DisplayName("ForceRevoke")
    class ForceRevoke {

        @Test
        @DisplayName("관리자가 강제로 무효화하면 해당 멤버의 리프레시 토큰을 삭제한다.")
        void shouldDeleteRefreshTokenWhenForceRevokedByAdmin() {
            // when
            forceRevokeService.forceRevoke(MEMBER_ID);

            // then
            verify(refreshTokenRepository).delete(MEMBER_ID);
        }
    }
}
