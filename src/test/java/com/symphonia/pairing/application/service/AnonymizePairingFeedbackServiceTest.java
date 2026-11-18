package com.symphonia.pairing.application.service;

import static org.mockito.BDDMockito.then;

import com.symphonia.UnitTest;
import com.symphonia.pairing.domain.repository.PairingFeedbackRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("AnonymizePairingFeedbackService 단위 테스트")
class AnonymizePairingFeedbackServiceTest extends UnitTest {

    private static final Long MEMBER_ID = 1L;

    @InjectMocks private AnonymizePairingFeedbackService anonymizePairingFeedbackService;

    @Mock private PairingFeedbackRepository pairingFeedbackRepository;

    @Nested
    @DisplayName("anonymize 메서드는")
    class Anonymize {

        @Test
        @DisplayName("회원이 낸 피드백의 회원 id를 비운다")
        void shouldAnonymizeFeedbacksOfMember() {
            // when
            anonymizePairingFeedbackService.anonymize(MEMBER_ID);

            // then
            then(pairingFeedbackRepository).should().anonymizeByMemberId(MEMBER_ID);
        }
    }
}
