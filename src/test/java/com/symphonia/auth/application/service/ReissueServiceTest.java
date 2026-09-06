package com.symphonia.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.symphonia.UnitTest;
import com.symphonia.auth.application.dto.result.TokenResult;
import com.symphonia.auth.application.usecase.IssueTokenUseCase;
import com.symphonia.auth.domain.error.AuthErrorCode;
import com.symphonia.auth.domain.repository.RefreshTokenRepository;
import com.symphonia.global.exception.BusinessException;
import com.symphonia.member.application.dto.result.MemberResult;
import com.symphonia.member.application.usecase.GetMemberUseCase;
import com.symphonia.member.domain.entity.Role;
import com.symphonia.member.domain.entity.SocialProvider;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("ReissueService 단위 테스트")
class ReissueServiceTest extends UnitTest {

    @InjectMocks private ReissueService reissueService;

    @Mock private RefreshTokenRepository refreshTokenRepository;

    @Mock private IssueTokenUseCase issueTokenUseCase;

    @Mock private GetMemberUseCase getMemberUseCase;

    private static final Long MEMBER_ID = 1L;
    private static final String REFRESH_TOKEN = "refresh-token";
    private static final String NEW_ACCESS_TOKEN = "new-access-token";
    private static final String NEW_REFRESH_TOKEN = "new-refresh-token";

    @Nested
    @DisplayName("Reissue")
    class Reissue {

        @Nested
        @DisplayName("리프레시 토큰에 해당하는 멤버가 존재하는 경우")
        class WhenRefreshTokenExists {
            private final MemberResult member =
                    new MemberResult(
                            MEMBER_ID,
                            "socialId",
                            "nickname",
                            "email",
                            "profileImage",
                            Role.ROLE_MEMBER,
                            SocialProvider.KAKAO);

            @BeforeEach
            void setUp() {
                given(refreshTokenRepository.findMemberIdByValue(REFRESH_TOKEN))
                        .willReturn(Optional.of(String.valueOf(MEMBER_ID)));
                given(getMemberUseCase.getById(MEMBER_ID)).willReturn(member);
                given(issueTokenUseCase.issue(String.valueOf(MEMBER_ID), member.role().name()))
                        .willReturn(TokenResult.of(NEW_ACCESS_TOKEN, NEW_REFRESH_TOKEN));
            }

            @Test
            @DisplayName("기존 리프레시 토큰을 삭제한다.")
            void shouldDeleteExistingRefreshToken() {
                // when
                reissueService.reissue(REFRESH_TOKEN);

                // then
                verify(refreshTokenRepository).delete(String.valueOf(MEMBER_ID));
            }

            @Test
            @DisplayName("새로운 TokenResult를 반환한다.")
            void shouldReturnNewTokenResult() {
                // when
                TokenResult result = reissueService.reissue(REFRESH_TOKEN);

                // then
                assertThat(result.accessToken()).isEqualTo(NEW_ACCESS_TOKEN);
                assertThat(result.refreshToken()).isEqualTo(NEW_REFRESH_TOKEN);
            }
        }

        @Nested
        @DisplayName("리프레시 토큰에 해당하는 멤버가 없는 경우")
        class WhenRefreshTokenNotFound {

            @Test
            @DisplayName("예외가 발생한다.")
            void shouldThrowException() {
                // given
                given(refreshTokenRepository.findMemberIdByValue(REFRESH_TOKEN))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> reissueService.reissue(REFRESH_TOKEN))
                        .isInstanceOf(BusinessException.class)
                        .hasMessage(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND.getMessage());
            }
        }
    }
}
