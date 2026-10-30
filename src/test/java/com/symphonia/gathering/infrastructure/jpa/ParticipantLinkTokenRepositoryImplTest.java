package com.symphonia.gathering.infrastructure.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import com.symphonia.RepositoryTest;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.domain.repository.ParticipantLinkTokenRepository;
import com.symphonia.gathering.fixture.GatheringFixture;
import com.symphonia.gathering.fixture.ParticipantFixture;
import com.symphonia.gathering.infrastructure.crypto.GatheringMasterKey;
import com.symphonia.gathering.infrastructure.crypto.config.GatheringCryptoConfig;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import({
    ParticipantLinkTokenRepositoryImpl.class,
    GatheringRepositoryImpl.class,
    GatheringDataKeyLoader.class,
    GatheringMasterKey.class,
    GatheringCryptoConfig.class
})
class ParticipantLinkTokenRepositoryImplTest extends RepositoryTest {

    private static final String VALUE = "link-token-value";

    @Autowired private ParticipantLinkTokenRepository participantLinkTokenRepository;
    @Autowired private ParticipantLinkTokenJpaRepository participantLinkTokenJpaRepository;
    @Autowired private GatheringRepository gatheringRepository;

    private Long participantId;

    @BeforeEach
    void setUp() {
        participantId =
                gatheringRepository
                        .save(
                                GatheringFixture.FRIDAY_DINNER.createWithParticipants(
                                        List.of(ParticipantFixture.KIM.create())))
                        .getParticipants()
                        .getFirst()
                        .getId();
    }

    @Nested
    @DisplayName("save 메서드는")
    class Save {

        @Test
        @DisplayName("개인 링크 토큰을 저장한다")
        void shouldPersistLinkToken() {
            // when
            participantLinkTokenRepository.save(VALUE, participantId);

            // then
            assertThat(participantLinkTokenRepository.findParticipantIdByValue(VALUE))
                    .contains(participantId);
        }

        @Test
        @DisplayName("원본 토큰 값이 아니라 해시된 값을 저장한다")
        void shouldPersistHashedValueInsteadOfRawValue() {
            // when
            participantLinkTokenRepository.save(VALUE, participantId);

            // then
            assertThat(participantLinkTokenJpaRepository.findByTokenHash(VALUE)).isEmpty();
        }
    }

    @Nested
    @DisplayName("findParticipantIdByValue 메서드는")
    class FindParticipantIdByValue {

        @Test
        @DisplayName("존재하지 않는 값이면 빈 Optional을 반환한다")
        void shouldReturnEmptyWhenValueNotExists() {
            // when
            Optional<Long> result =
                    participantLinkTokenRepository.findParticipantIdByValue("unknown");

            // then
            assertThat(result).isEmpty();
        }
    }
}
