package com.symphonia.gathering.infrastructure.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.symphonia.RepositoryTest;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.exception.ParticipantAlreadyJoinedException;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.domain.repository.ParticipantGoogleSubjectRepository;
import com.symphonia.gathering.fixture.GatheringFixture;
import com.symphonia.gathering.fixture.ParticipantFixture;
import com.symphonia.gathering.infrastructure.crypto.GatheringMasterKey;
import com.symphonia.gathering.infrastructure.crypto.config.GatheringCryptoConfig;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@Import({
    ParticipantGoogleSubjectRepositoryImpl.class,
    GatheringRepositoryImpl.class,
    GatheringDataKeyLoader.class,
    GatheringMasterKey.class,
    GatheringCryptoConfig.class
})
class ParticipantGoogleSubjectRepositoryImplTest extends RepositoryTest {

    private static final String GOOGLE_SUBJECT = "109876543210987654321";
    private static final String OTHER_GOOGLE_SUBJECT = "109876543210987654322";
    private static final LocalDateTime WITHDRAWN_AT = LocalDateTime.of(2026, 11, 22, 10, 0);

    @Autowired private ParticipantGoogleSubjectRepository participantGoogleSubjectRepository;
    @Autowired private GatheringRepository gatheringRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private Long gatheringId;
    private Long firstParticipantId;
    private Long secondParticipantId;

    @BeforeEach
    void setUp() {
        Gathering gathering =
                gatheringRepository.save(
                        GatheringFixture.FRIDAY_DINNER.createWithParticipants(
                                List.of(
                                        ParticipantFixture.KIM.create(),
                                        ParticipantFixture.LEE.create())));
        gatheringId = gathering.getId();
        firstParticipantId = gathering.getParticipants().get(0).getId();
        secondParticipantId = gathering.getParticipants().get(1).getId();
    }

    @Nested
    @DisplayName("exists 메서드는")
    class Exists {

        @Test
        @DisplayName("저장한 구글 계정이면 true를 반환한다")
        void shouldReturnTrueWhenSubjectSaved() {
            // given
            participantGoogleSubjectRepository.save(
                    gatheringId, firstParticipantId, GOOGLE_SUBJECT);

            // when
            boolean exists = participantGoogleSubjectRepository.exists(gatheringId, GOOGLE_SUBJECT);

            // then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("저장하지 않은 구글 계정이면 false를 반환한다")
        void shouldReturnFalseWhenSubjectNotSaved() {
            // given
            participantGoogleSubjectRepository.save(
                    gatheringId, firstParticipantId, GOOGLE_SUBJECT);

            // when
            boolean exists =
                    participantGoogleSubjectRepository.exists(gatheringId, OTHER_GOOGLE_SUBJECT);

            // then
            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("다른 모임에 저장한 구글 계정이면 false를 반환한다")
        void shouldReturnFalseWhenSubjectSavedInOtherGathering() {
            // given
            Long otherGatheringId =
                    gatheringRepository
                            .save(
                                    GatheringFixture.FRIDAY_DINNER.createWithParticipants(
                                            List.of(ParticipantFixture.KIM.create())))
                            .getId();
            participantGoogleSubjectRepository.save(
                    gatheringId, firstParticipantId, GOOGLE_SUBJECT);

            // when
            boolean exists =
                    participantGoogleSubjectRepository.exists(otherGatheringId, GOOGLE_SUBJECT);

            // then
            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("익명화한 참여자의 구글 계정이면 false를 반환한다")
        void shouldReturnFalseWhenParticipantAnonymized() {
            // given
            participantGoogleSubjectRepository.save(
                    gatheringId, firstParticipantId, GOOGLE_SUBJECT);
            gatheringRepository.anonymizeParticipant(firstParticipantId, WITHDRAWN_AT);

            // when
            boolean exists = participantGoogleSubjectRepository.exists(gatheringId, GOOGLE_SUBJECT);

            // then
            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("findParticipantId 메서드는")
    class FindParticipantId {

        @Test
        @DisplayName("저장한 구글 계정이면 그 참여자의 id를 반환한다")
        void shouldReturnParticipantIdWhenSubjectSaved() {
            // given
            participantGoogleSubjectRepository.save(
                    gatheringId, secondParticipantId, GOOGLE_SUBJECT);

            // when
            Optional<Long> participantId =
                    participantGoogleSubjectRepository.findParticipantId(
                            gatheringId, GOOGLE_SUBJECT);

            // then
            assertThat(participantId).contains(secondParticipantId);
        }

        @Test
        @DisplayName("저장하지 않은 구글 계정이면 빈 Optional을 반환한다")
        void shouldReturnEmptyWhenSubjectNotSaved() {
            // given
            participantGoogleSubjectRepository.save(
                    gatheringId, firstParticipantId, GOOGLE_SUBJECT);

            // when
            Optional<Long> participantId =
                    participantGoogleSubjectRepository.findParticipantId(
                            gatheringId, OTHER_GOOGLE_SUBJECT);

            // then
            assertThat(participantId).isEmpty();
        }

        @Test
        @DisplayName("다른 모임에 저장한 구글 계정이면 빈 Optional을 반환한다")
        void shouldReturnEmptyWhenSubjectSavedInOtherGathering() {
            // given
            Long otherGatheringId =
                    gatheringRepository
                            .save(
                                    GatheringFixture.FRIDAY_DINNER.createWithParticipants(
                                            List.of(ParticipantFixture.KIM.create())))
                            .getId();
            participantGoogleSubjectRepository.save(
                    gatheringId, firstParticipantId, GOOGLE_SUBJECT);

            // when
            Optional<Long> participantId =
                    participantGoogleSubjectRepository.findParticipantId(
                            otherGatheringId, GOOGLE_SUBJECT);

            // then
            assertThat(participantId).isEmpty();
        }

        @Test
        @DisplayName("익명화한 참여자의 구글 계정이면 빈 Optional을 반환한다")
        void shouldReturnEmptyWhenParticipantAnonymized() {
            // given
            participantGoogleSubjectRepository.save(
                    gatheringId, firstParticipantId, GOOGLE_SUBJECT);
            gatheringRepository.anonymizeParticipant(firstParticipantId, WITHDRAWN_AT);

            // when
            Optional<Long> participantId =
                    participantGoogleSubjectRepository.findParticipantId(
                            gatheringId, GOOGLE_SUBJECT);

            // then
            assertThat(participantId).isEmpty();
        }

        @Test
        @DisplayName("구글 계정 없이 참여한 참여자만 있으면 빈 Optional을 반환한다")
        void shouldReturnEmptyWhenParticipantHasNoSubject() {
            // when
            Optional<Long> participantId =
                    participantGoogleSubjectRepository.findParticipantId(
                            gatheringId, GOOGLE_SUBJECT);

            // then
            assertThat(participantId).isEmpty();
        }
    }

    @Nested
    @DisplayName("save 메서드는")
    class Save {

        @Test
        @DisplayName("구글 sub 원문이 아니라 64자리 HMAC을 저장한다")
        void shouldPersistHmacInsteadOfRawSubject() {
            // when
            participantGoogleSubjectRepository.save(
                    gatheringId, firstParticipantId, GOOGLE_SUBJECT);

            // then
            String stored =
                    jdbcTemplate.queryForObject(
                            "SELECT google_sub_hmac FROM participant WHERE id = ?",
                            String.class,
                            firstParticipantId);
            assertThat(stored).hasSize(64).isNotEqualTo(GOOGLE_SUBJECT);
        }

        @Test
        @DisplayName("같은 구글 계정을 쓰던 참여자가 익명화된 뒤에는 다른 참여자가 같은 구글 계정을 저장할 수 있다")
        void shouldSaveSubjectWhenPreviousParticipantAnonymized() {
            // given
            participantGoogleSubjectRepository.save(
                    gatheringId, firstParticipantId, GOOGLE_SUBJECT);
            gatheringRepository.anonymizeParticipant(firstParticipantId, WITHDRAWN_AT);

            // when
            participantGoogleSubjectRepository.save(
                    gatheringId, secondParticipantId, GOOGLE_SUBJECT);

            // then
            assertThat(
                            participantGoogleSubjectRepository.findParticipantId(
                                    gatheringId, GOOGLE_SUBJECT))
                    .contains(secondParticipantId);
        }

        @Test
        @DisplayName("같은 모임의 다른 참여자가 같은 구글 계정을 쓰고 있으면 예외를 던진다")
        void shouldThrowParticipantAlreadyJoinedExceptionWhenSubjectDuplicated() {
            // given
            participantGoogleSubjectRepository.save(
                    gatheringId, firstParticipantId, GOOGLE_SUBJECT);

            // when & then
            assertThatThrownBy(
                            () ->
                                    participantGoogleSubjectRepository.save(
                                            gatheringId, secondParticipantId, GOOGLE_SUBJECT))
                    .isInstanceOf(ParticipantAlreadyJoinedException.class);
        }
    }
}
