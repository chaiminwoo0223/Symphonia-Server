package com.symphonia.pairing.infrastructure.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import com.symphonia.RepositoryTest;
import com.symphonia.pairing.domain.entity.PairingFeedback;
import com.symphonia.pairing.domain.repository.PairingFeedbackRepository;
import com.symphonia.pairing.domain.vo.MoodType;
import com.symphonia.pairing.domain.vo.RelationshipType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(PairingFeedbackRepositoryImpl.class)
class PairingFeedbackRepositoryImplTest extends RepositoryTest {

    @Autowired private PairingFeedbackRepository pairingFeedbackRepository;

    @Nested
    @DisplayName("save 메서드는")
    class Save {

        @Test
        @DisplayName("피드백을 저장하고 ID를 채워 반환한다")
        void shouldPersistPairingFeedback() {
            // given
            PairingFeedback pairingFeedback =
                    PairingFeedback.of(1L, 1L, 1L, 1L, RelationshipType.FRIEND, MoodType.CASUAL);

            // when
            PairingFeedback saved = pairingFeedbackRepository.save(pairingFeedback);

            // then
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getMemberId()).isEqualTo(1L);
            assertThat(saved.getRelationshipType()).isEqualTo(RelationshipType.FRIEND);
            assertThat(saved.getMoodType()).isEqualTo(MoodType.CASUAL);
        }
    }
}
