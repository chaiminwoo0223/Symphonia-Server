package com.symphonia.gathering.infrastructure.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import com.symphonia.RepositoryTest;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.repository.GatheringPairingRepository;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.domain.vo.GatheringPairing;
import com.symphonia.gathering.fixture.GatheringFixture;
import com.symphonia.gathering.fixture.GatheringPairingFixture;
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
    GatheringPairingRepositoryImpl.class,
    GatheringRepositoryImpl.class,
    GatheringDataKeyLoader.class,
    GatheringMasterKey.class,
    GatheringCryptoConfig.class
})
class GatheringPairingRepositoryImplTest extends RepositoryTest {

    @Autowired private GatheringPairingRepository gatheringPairingRepository;
    @Autowired private GatheringPairingJpaRepository gatheringPairingJpaRepository;
    @Autowired private GatheringRepository gatheringRepository;

    private Long gatheringId;
    private Long participantId;
    private Long otherGatheringId;
    private Long otherParticipantId;

    @BeforeEach
    void setUp() {
        Gathering gathering = saveGatheringWithParticipant();
        Gathering otherGathering = saveGatheringWithParticipant();
        gatheringId = gathering.getId();
        participantId = gathering.getParticipants().getFirst().getId();
        otherGatheringId = otherGathering.getId();
        otherParticipantId = otherGathering.getParticipants().getFirst().getId();
    }

    @Nested
    @DisplayName("save 메서드는")
    class Save {

        @Nested
        @DisplayName("모임에서 고른 페어링이 없는 경우")
        class WhenPairingAbsent {

            @Test
            @DisplayName("모임의 페어링을 저장한다")
            void shouldSavePairing() {
                // given
                GatheringPairing pairing = GatheringPairingFixture.FRIEND_CASUAL.create();

                // when
                gatheringPairingRepository.save(gatheringId, pairing);

                // then
                assertThat(gatheringPairingJpaRepository.findById(gatheringId))
                        .map(GatheringPairingJpaEntity::toDomain)
                        .hasValue(pairing);
            }
        }

        @Nested
        @DisplayName("모임에서 고른 페어링이 이미 있는 경우")
        class WhenPairingAlreadyChosen {

            @Test
            @DisplayName("기본 키 위반 없이 같은 행을 새 페어링으로 덮어쓴다")
            void shouldOverwritePairing() {
                // given
                gatheringPairingRepository.save(
                        gatheringId, GatheringPairingFixture.FRIEND_CASUAL.create());
                GatheringPairing changed = GatheringPairingFixture.LOVER_ROMANTIC.create();

                // when
                gatheringPairingRepository.save(gatheringId, changed);

                // then
                assertThat(gatheringPairingJpaRepository.findById(gatheringId))
                        .map(GatheringPairingJpaEntity::toDomain)
                        .hasValue(changed);
            }
        }
    }

    @Nested
    @DisplayName("findByParticipantId 메서드는")
    class FindByParticipantId {

        @Nested
        @DisplayName("참여자가 속한 모임에서 고른 페어링이 있는 경우")
        class WhenPairingChosen {

            @Test
            @DisplayName("다른 모임의 페어링이 아니라 참여자가 속한 모임의 페어링을 반환한다")
            void shouldReturnPairingOfParticipantGathering() {
                // given
                gatheringPairingRepository.save(
                        gatheringId, GatheringPairingFixture.FRIEND_CASUAL.create());
                gatheringPairingRepository.save(
                        otherGatheringId, GatheringPairingFixture.LOVER_ROMANTIC.create());

                // when
                Optional<GatheringPairing> result =
                        gatheringPairingRepository.findByParticipantId(participantId);

                // then
                assertThat(result).hasValue(GatheringPairingFixture.FRIEND_CASUAL.create());
            }
        }

        @Nested
        @DisplayName("참여자가 속한 모임에서 고른 페어링이 없는 경우")
        class WhenPairingAbsent {

            @Test
            @DisplayName("다른 모임에 페어링이 있어도 빈 Optional을 반환한다")
            void shouldReturnEmpty() {
                // given
                gatheringPairingRepository.save(
                        otherGatheringId, GatheringPairingFixture.LOVER_ROMANTIC.create());

                // when
                Optional<GatheringPairing> result =
                        gatheringPairingRepository.findByParticipantId(participantId);

                // then
                assertThat(result).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("deleteByGatheringId 메서드는")
    class DeleteByGatheringId {

        @Test
        @DisplayName("모임의 페어링만 지우고 다른 모임의 페어링은 남긴다")
        void shouldDeletePairingOfGatheringOnly() {
            // given
            gatheringPairingRepository.save(
                    gatheringId, GatheringPairingFixture.FRIEND_CASUAL.create());
            gatheringPairingRepository.save(
                    otherGatheringId, GatheringPairingFixture.LOVER_ROMANTIC.create());

            // when
            gatheringPairingRepository.deleteByGatheringId(gatheringId);

            // then
            assertThat(gatheringPairingRepository.findByParticipantId(participantId)).isEmpty();
            assertThat(gatheringPairingRepository.findByParticipantId(otherParticipantId))
                    .hasValue(GatheringPairingFixture.LOVER_ROMANTIC.create());
        }
    }

    private Gathering saveGatheringWithParticipant() {
        return gatheringRepository.save(
                GatheringFixture.FRIDAY_DINNER.createWithParticipants(
                        List.of(ParticipantFixture.KIM.create())));
    }
}
