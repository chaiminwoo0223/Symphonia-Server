package com.symphonia.pairing.application.listener;

import static org.mockito.BDDMockito.then;

import com.symphonia.UnitTest;
import com.symphonia.member.application.event.MemberDeletedEvent;
import com.symphonia.pairing.application.usecase.AnonymizePairingFeedbackUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("PairingMemberDeletedEventListener 단위 테스트")
class PairingMemberDeletedEventListenerTest extends UnitTest {

    private static final Long MEMBER_ID = 1L;

    @InjectMocks private PairingMemberDeletedEventListener pairingMemberDeletedEventListener;

    @Mock private AnonymizePairingFeedbackUseCase anonymizePairingFeedbackUseCase;

    @Nested
    @DisplayName("handle 메서드는")
    class Handle {

        @Test
        @DisplayName("멤버 삭제 이벤트를 받으면 탈퇴한 회원의 피드백을 익명화한다")
        void shouldAnonymizeFeedbacksWhenMemberDeletedEventReceived() {
            // given
            MemberDeletedEvent event =
                    MemberDeletedEvent.of(MEMBER_ID, "access-token", "127.0.0.1");

            // when
            pairingMemberDeletedEventListener.handle(event);

            // then
            then(anonymizePairingFeedbackUseCase).should().anonymize(MEMBER_ID);
        }
    }
}
