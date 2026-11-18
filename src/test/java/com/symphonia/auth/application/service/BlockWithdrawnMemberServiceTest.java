package com.symphonia.auth.application.service;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.symphonia.UnitTest;
import com.symphonia.auth.domain.provider.AccessTokenProvider;
import com.symphonia.auth.domain.repository.WithdrawnMemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("BlockWithdrawnMemberService 단위 테스트")
class BlockWithdrawnMemberServiceTest extends UnitTest {

    private static final String MEMBER_ID = "1";
    private static final long ACCESS_TOKEN_EXPIRATION_TIME = 7200L;

    @InjectMocks private BlockWithdrawnMemberService blockWithdrawnMemberService;

    @Mock private AccessTokenProvider accessTokenProvider;
    @Mock private WithdrawnMemberRepository withdrawnMemberRepository;

    @Nested
    @DisplayName("block 메서드는")
    class Block {

        @Test
        @DisplayName("탈퇴한 회원 id를 액세스 토큰 유효시간만큼 저장한다")
        void shouldSaveWithdrawnMemberForAccessTokenExpirationTime() {
            // given
            given(accessTokenProvider.getExpirationTime()).willReturn(ACCESS_TOKEN_EXPIRATION_TIME);

            // when
            blockWithdrawnMemberService.block(MEMBER_ID);

            // then
            then(withdrawnMemberRepository).should().save(MEMBER_ID, ACCESS_TOKEN_EXPIRATION_TIME);
        }
    }
}
