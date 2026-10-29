package com.symphonia.gathering.infrastructure.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.symphonia.RepositoryTest;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.entity.Participant;
import com.symphonia.gathering.domain.exception.ParticipantAlreadyJoinedException;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.domain.vo.PhoneNumber;
import com.symphonia.gathering.fixture.GatheringFixture;
import com.symphonia.gathering.fixture.ParticipantFixture;
import com.symphonia.gathering.infrastructure.crypto.GatheringMasterKey;
import com.symphonia.gathering.infrastructure.crypto.config.GatheringCryptoConfig;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import({GatheringRepositoryImpl.class, GatheringMasterKey.class, GatheringCryptoConfig.class})
class GatheringRepositoryImplTest extends RepositoryTest {

    @Autowired private GatheringRepository gatheringRepository;
    @Autowired private ParticipantJpaRepository participantJpaRepository;
    @Autowired private GatheringDataKeyJpaRepository gatheringDataKeyJpaRepository;
    @Autowired private EntityManager entityManager;

    @Nested
    @DisplayName("save 메서드는")
    class Save {

        @Nested
        @DisplayName("처음 저장하는 모임인 경우")
        class WhenGatheringIsNew {

            @Test
            @DisplayName("모임과 함께 모임별 데이터 키를 저장한다")
            void shouldPersistGatheringWithDataKey() {
                // given
                Gathering gathering = GatheringFixture.FRIDAY_DINNER.create();

                // when
                Gathering saved = gatheringRepository.save(gathering);

                // then
                assertThat(saved.getId()).isNotNull();
                assertThat(gatheringDataKeyJpaRepository.findById(saved.getId())).isPresent();
            }

            @Test
            @DisplayName("참여자의 이름과 휴대폰을 평문 없이 암호화해 저장한다")
            void shouldEncryptParticipantNameAndPhoneNumber() {
                // given
                Gathering gathering =
                        GatheringFixture.FRIDAY_DINNER.createWithParticipants(
                                List.of(ParticipantFixture.KIM.create()));

                // when
                Gathering saved = gatheringRepository.save(gathering);

                // then
                ParticipantJpaEntity stored =
                        participantJpaRepository
                                .findAllByGatheringIdOrderById(saved.getId())
                                .getFirst();
                assertThat(new String(stored.getEncryptedName(), StandardCharsets.UTF_8))
                        .doesNotContain(ParticipantFixture.KIM.getName());
                assertThat(new String(stored.getEncryptedPhoneNumber(), StandardCharsets.UTF_8))
                        .doesNotContain(ParticipantFixture.KIM.getPhoneNumber());
                assertThat(stored.getPhoneNumberHmac())
                        .doesNotContain(ParticipantFixture.KIM.getPhoneNumber());
            }
        }

        @Nested
        @DisplayName("이미 저장된 모임에 참여자를 더한 경우")
        class WhenParticipantJoinsSavedGathering {

            @Test
            @DisplayName("기존 참여자를 유지하고 새 참여자를 함께 저장한다")
            void shouldPersistJoinedParticipant() {
                // given
                Gathering saved =
                        gatheringRepository.save(
                                GatheringFixture.FRIDAY_DINNER.createWithParticipants(
                                        List.of(ParticipantFixture.KIM.create())));
                clearPersistenceContext();
                Gathering loaded = gatheringRepository.findById(saved.getId()).orElseThrow();
                loaded.join(
                        ParticipantFixture.LEE.getName(),
                        new PhoneNumber(ParticipantFixture.LEE.getPhoneNumber()),
                        ParticipantFixture.LEE.isNonDrinker(),
                        loaded.getCreatedAt().plusHours(1));

                // when
                gatheringRepository.save(loaded);

                // then
                clearPersistenceContext();
                assertThat(
                                gatheringRepository
                                        .findById(saved.getId())
                                        .orElseThrow()
                                        .getParticipants())
                        .extracting(Participant::getName)
                        .containsExactly(
                                ParticipantFixture.KIM.getName(), ParticipantFixture.LEE.getName());
            }
        }

        @Nested
        @DisplayName("같은 모임에 같은 휴대폰의 참여자가 둘인 경우")
        class WhenPhoneNumberDuplicated {

            @Test
            @DisplayName("ParticipantAlreadyJoinedException이 발생한다")
            void shouldThrowParticipantAlreadyJoinedException() {
                // given: 동시 참여로 Service의 중복 검사를 둘 다 통과한 상태
                Gathering gathering =
                        GatheringFixture.FRIDAY_DINNER.createWithParticipants(
                                List.of(
                                        ParticipantFixture.KIM.create(),
                                        ParticipantFixture.LEE.createWithPhoneNumber(
                                                ParticipantFixture.KIM.getPhoneNumber())));

                // when & then
                assertThatThrownBy(() -> gatheringRepository.save(gathering))
                        .isInstanceOf(ParticipantAlreadyJoinedException.class);
            }
        }
    }

    @Nested
    @DisplayName("findById 메서드는")
    class FindById {

        @Test
        @DisplayName("존재하는 ID면 참여자의 이름과 휴대폰을 복호화한 모임을 반환한다")
        void shouldReturnDecryptedGatheringWhenIdExists() {
            // given
            Gathering saved =
                    gatheringRepository.save(
                            GatheringFixture.FRIDAY_DINNER.createWithParticipants(
                                    List.of(ParticipantFixture.KIM.create())));
            clearPersistenceContext();

            // when
            Optional<Gathering> result = gatheringRepository.findById(saved.getId());

            // then
            assertThat(result).isPresent();
            Participant participant = result.get().getParticipants().getFirst();
            assertThat(participant.getName()).isEqualTo(ParticipantFixture.KIM.getName());
            assertThat(participant.getPhoneNumber().value())
                    .isEqualTo(ParticipantFixture.KIM.getPhoneNumber());
        }

        @Test
        @DisplayName("존재하지 않는 ID면 빈 Optional을 반환한다")
        void shouldReturnEmptyWhenIdNotExists() {
            // when
            Optional<Gathering> result = gatheringRepository.findById(-1L);

            // then
            assertThat(result).isEmpty();
        }
    }

    // 1차 캐시를 비워 다음 조회가 DB의 암호문을 다시 읽고 복호화하게 한다.
    private void clearPersistenceContext() {
        entityManager.flush();
        entityManager.clear();
    }
}
