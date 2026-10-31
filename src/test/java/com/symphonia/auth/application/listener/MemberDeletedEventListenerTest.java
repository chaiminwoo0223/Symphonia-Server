package com.symphonia.auth.application.listener;

import static org.mockito.BDDMockito.then;

import com.symphonia.UnitTest;
import com.symphonia.auth.application.dto.command.LogoutCommand;
import com.symphonia.auth.application.usecase.ForceRevokeUseCase;
import com.symphonia.auth.application.usecase.LogoutUseCase;
import com.symphonia.member.application.event.MemberDeletedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("MemberDeletedEventListener 단위 테스트")
class MemberDeletedEventListenerTest extends UnitTest {

    private static final Long MEMBER_ID = 1L;
    private static final String ACCESS_TOKEN = "access-token";
    private static final String IP = "127.0.0.1";

    @InjectMocks private MemberDeletedEventListener memberDeletedEventListener;

    @Mock private ForceRevokeUseCase forceRevokeUseCase;

    @Mock private LogoutUseCase logoutUseCase;

    @Nested
    @DisplayName("handle 메서드는")
    class Handle {

        @Test
        @DisplayName("멤버 삭제 이벤트를 받으면 모든 기기의 리프레시 토큰을 지우고 요청한 액세스 토큰을 로그아웃 처리한다.")
        void shouldForceRevokeAndLogoutWhenMemberDeletedEventReceived() {
            // given
            MemberDeletedEvent event = MemberDeletedEvent.of(MEMBER_ID, ACCESS_TOKEN, IP);

            // when
            memberDeletedEventListener.handle(event);

            // then
            then(forceRevokeUseCase).should().forceRevoke(String.valueOf(MEMBER_ID));
            then(logoutUseCase).should().logout(LogoutCommand.of(null, ACCESS_TOKEN, IP));
        }
    }
}
