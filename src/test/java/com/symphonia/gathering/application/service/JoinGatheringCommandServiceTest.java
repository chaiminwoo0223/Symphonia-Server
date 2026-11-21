package com.symphonia.gathering.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

import com.symphonia.UnitTest;
import com.symphonia.gathering.application.dto.command.AddParticipantCommand;
import com.symphonia.gathering.application.dto.command.JoinGatheringAsHostCommand;
import com.symphonia.gathering.application.dto.result.ParticipantResult;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.entity.Participant;
import com.symphonia.gathering.domain.exception.ConsentRequiredException;
import com.symphonia.gathering.domain.exception.GatheringJoinClosedException;
import com.symphonia.gathering.domain.exception.GatheringNotFoundException;
import com.symphonia.gathering.domain.exception.GatheringParticipantCountExceedsLimitException;
import com.symphonia.gathering.domain.exception.InvalidPhoneNumberException;
import com.symphonia.gathering.domain.exception.JoinTokenNotFoundException;
import com.symphonia.gathering.domain.exception.NotGatheringHostException;
import com.symphonia.gathering.domain.exception.OutdatedConsentVersionException;
import com.symphonia.gathering.domain.exception.ParticipantAlreadyJoinedException;
import com.symphonia.gathering.domain.policy.GatheringPolicy;
import com.symphonia.gathering.domain.provider.GatheringTokenProvider;
import com.symphonia.gathering.domain.repository.GatheringJoinTokenRepository;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.domain.repository.GatheringRetentionRepository;
import com.symphonia.gathering.domain.repository.ParticipantGoogleSubjectRepository;
import com.symphonia.gathering.domain.repository.ParticipantLinkTokenRepository;
import com.symphonia.gathering.domain.vo.PhoneNumber;
import com.symphonia.gathering.fixture.GatheringFixture;
import com.symphonia.gathering.fixture.GatheringRetentionFixture;
import com.symphonia.gathering.fixture.ParticipantFixture;
import com.symphonia.global.config.TimeConfig;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

@DisplayName("JoinGatheringCommandService 단위 테스트")
class JoinGatheringCommandServiceTest extends UnitTest {

    private static final Long GATHERING_ID = 10L;
    private static final Long PARTICIPANT_ID = 20L;
    private static final Long OTHER_MEMBER_ID = 99L;
    private static final String JOIN_TOKEN = "join-token";
    private static final String GOOGLE_SUBJECT = "109876543210987654321";
    private static final String LINK_TOKEN = "link-token";
    private static final String HYPHENATED_PHONE_NUMBER = "010-1234-5678";
    private static final String MASKED_PHONE_NUMBER = "010-****-5678";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 29, 18, 0);

    // 참여 기간은 모임을 만든 지 48시간이 되는 순간 닫힌다.
    // 닫히기 1초 전과 닫히는 순간을 경계로 쓴다.
    private static final LocalDateTime OPEN_PERIOD_OPENED_AT = NOW.minusHours(48).plusSeconds(1);
    private static final LocalDateTime CLOSED_PERIOD_OPENED_AT = NOW.minusHours(48);

    private JoinGatheringCommandService joinGatheringCommandService;

    @Mock private GatheringRepository gatheringRepository;
    @Mock private GatheringJoinTokenRepository gatheringJoinTokenRepository;
    @Mock private ParticipantLinkTokenRepository participantLinkTokenRepository;
    @Mock private ParticipantGoogleSubjectRepository participantGoogleSubjectRepository;
    @Mock private GatheringRetentionRepository gatheringRetentionRepository;
    @Mock private GatheringTokenProvider gatheringTokenProvider;

    @BeforeEach
    void setUp() {
        joinGatheringCommandService =
                new JoinGatheringCommandService(
                        gatheringRepository,
                        gatheringJoinTokenRepository,
                        participantLinkTokenRepository,
                        participantGoogleSubjectRepository,
                        gatheringRetentionRepository,
                        gatheringTokenProvider,
                        Clock.fixed(
                                NOW.atZone(TimeConfig.SERVICE_ZONE).toInstant(),
                                TimeConfig.SERVICE_ZONE));
    }

    @Nested
    @DisplayName("add 메서드는")
    class Add {

        @Nested
        @DisplayName("참여 토큰에 해당하는 모임이 없는 경우")
        class WhenJoinTokenNotFound {

            @Test
            @DisplayName("예외를 던지고 참여자와 개인 링크 토큰을 저장하지 않는다")
            void shouldThrowJoinTokenNotFoundException() {
                // given
                given(gatheringJoinTokenRepository.findGatheringIdByValue(JOIN_TOKEN))
                        .willReturn(Optional.empty());
                AddParticipantCommand command = command(GatheringPolicy.CONSENT_VERSION);

                // when & then
                assertThatThrownBy(() -> joinGatheringCommandService.add(command))
                        .isInstanceOf(JoinTokenNotFoundException.class);
                thenNothingSaved();
            }
        }

        @Nested
        @DisplayName("모임의 파기 시각이 지난 경우")
        class WhenGatheringExpired {

            @Test
            @DisplayName("물리 삭제 전이라도 참여 토큰을 찾지 못한 예외를 던지고 모임을 읽거나 참여자를 저장하지 않는다")
            void shouldThrowJoinTokenNotFoundException() {
                // given
                given(gatheringJoinTokenRepository.findGatheringIdByValue(JOIN_TOKEN))
                        .willReturn(Optional.of(GATHERING_ID));
                givenRetention(GatheringRetentionFixture.EXPIRED);
                AddParticipantCommand command = command(GatheringPolicy.CONSENT_VERSION);

                // when & then
                assertThatThrownBy(() -> joinGatheringCommandService.add(command))
                        .isInstanceOf(JoinTokenNotFoundException.class);
                then(gatheringRepository).shouldHaveNoInteractions();
                then(participantLinkTokenRepository).shouldHaveNoInteractions();
                then(participantGoogleSubjectRepository).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("모임을 만든 지 48시간이 된 경우")
        class WhenRecordingPeriodClosed {

            @Test
            @DisplayName("예외를 던지고 참여자와 개인 링크 토큰을 저장하지 않는다")
            void shouldThrowGatheringJoinClosedException() {
                // given
                givenGathering(
                        GatheringFixture.FRIDAY_DINNER.createWithIdAndOpenedAt(
                                GATHERING_ID, CLOSED_PERIOD_OPENED_AT));
                AddParticipantCommand command = command(GatheringPolicy.CONSENT_VERSION);

                // when & then
                assertThatThrownBy(() -> joinGatheringCommandService.add(command))
                        .isInstanceOf(GatheringJoinClosedException.class);
                thenNothingSaved();
            }
        }

        @Nested
        @DisplayName("개인정보 수집 및 이용에 동의하지 않은 경우")
        class WhenConsentNotAgreed {

            @Test
            @DisplayName("예외를 던지고 참여자와 개인 링크 토큰을 저장하지 않는다")
            void shouldThrowConsentRequiredException() {
                // given
                givenJoinableGathering(List.of());
                AddParticipantCommand command = command(false, GatheringPolicy.CONSENT_VERSION);

                // when & then
                assertThatThrownBy(() -> joinGatheringCommandService.add(command))
                        .isInstanceOf(ConsentRequiredException.class);
                thenNothingSaved();
            }
        }

        @Nested
        @DisplayName("동의 안내 버전이 현재 버전과 다른 경우")
        class WhenConsentVersionOutdated {

            @Test
            @DisplayName("예외를 던지고 참여자와 개인 링크 토큰을 저장하지 않는다")
            void shouldThrowOutdatedConsentVersionException() {
                // given
                givenJoinableGathering(List.of());
                AddParticipantCommand command = command("2000-01-01");

                // when & then
                assertThatThrownBy(() -> joinGatheringCommandService.add(command))
                        .isInstanceOf(OutdatedConsentVersionException.class);
                thenNothingSaved();
            }
        }

        @Nested
        @DisplayName("같은 휴대폰으로 이미 참여한 경우")
        class WhenPhoneNumberAlreadyJoined {

            @Test
            @DisplayName("하이픈을 섞어 입력해도 중복으로 보고 예외를 던지며 아무것도 저장하지 않는다")
            void shouldThrowParticipantAlreadyJoinedException() {
                // given
                givenJoinableGathering(
                        List.of(ParticipantFixture.KIM.createWithId(PARTICIPANT_ID)));
                AddParticipantCommand command = command(GatheringPolicy.CONSENT_VERSION);

                // when & then
                assertThatThrownBy(() -> joinGatheringCommandService.add(command))
                        .isInstanceOf(ParticipantAlreadyJoinedException.class);
                thenNothingSaved();
            }
        }

        @Nested
        @DisplayName("참여자가 이미 20명인 경우")
        class WhenParticipantCountAtLimit {

            @Test
            @DisplayName("예외를 던지고 참여자와 개인 링크 토큰을 저장하지 않는다")
            void shouldThrowGatheringParticipantCountExceedsLimitException() {
                // given
                givenJoinableGathering(participantsOf(GatheringPolicy.MAX_PARTICIPANT_COUNT));
                AddParticipantCommand command = command(GatheringPolicy.CONSENT_VERSION);

                // when & then
                assertThatThrownBy(() -> joinGatheringCommandService.add(command))
                        .isInstanceOf(GatheringParticipantCountExceedsLimitException.class);
                thenNothingSaved();
            }
        }

        @Nested
        @DisplayName("참여자가 이미 상한을 넘은 모임인 경우")
        class WhenParticipantCountOverLimit {

            @Test
            @DisplayName("예외를 던지고 참여자와 개인 링크 토큰을 저장하지 않는다")
            void shouldThrowGatheringParticipantCountExceedsLimitException() {
                // given
                givenJoinableGathering(participantsOf(GatheringPolicy.MAX_PARTICIPANT_COUNT + 5));
                AddParticipantCommand command = command(GatheringPolicy.CONSENT_VERSION);

                // when & then
                assertThatThrownBy(() -> joinGatheringCommandService.add(command))
                        .isInstanceOf(GatheringParticipantCountExceedsLimitException.class);
                thenNothingSaved();
            }
        }

        @Nested
        @DisplayName("참여자가 20명인데 같은 휴대폰으로 다시 참여하는 경우")
        class WhenPhoneNumberAlreadyJoinedAtLimit {

            @Test
            @DisplayName("상한 초과가 아니라 이미 참여했다는 예외를 던진다")
            void shouldThrowParticipantAlreadyJoinedException() {
                // given
                List<Participant> participants =
                        Stream.concat(
                                        Stream.of(
                                                ParticipantFixture.KIM.createWithId(
                                                        PARTICIPANT_ID)),
                                        participantsOf(GatheringPolicy.MAX_PARTICIPANT_COUNT - 1)
                                                .stream())
                                .toList();
                givenJoinableGathering(participants);
                AddParticipantCommand command = command(GatheringPolicy.CONSENT_VERSION);

                // when & then
                assertThatThrownBy(() -> joinGatheringCommandService.add(command))
                        .isInstanceOf(ParticipantAlreadyJoinedException.class);
                thenNothingSaved();
            }
        }

        @Nested
        @DisplayName("같은 구글 계정으로 이미 참여한 경우")
        class WhenGoogleAccountAlreadyJoined {

            @Test
            @DisplayName("다른 휴대폰이어도 중복으로 보고 예외를 던지며 아무것도 저장하지 않는다")
            void shouldThrowParticipantAlreadyJoinedException() {
                // given
                givenJoinableGathering(List.of());
                given(participantGoogleSubjectRepository.exists(GATHERING_ID, GOOGLE_SUBJECT))
                        .willReturn(true);
                AddParticipantCommand command = command(GatheringPolicy.CONSENT_VERSION);

                // when & then
                assertThatThrownBy(() -> joinGatheringCommandService.add(command))
                        .isInstanceOf(ParticipantAlreadyJoinedException.class);
                thenNothingSaved();
            }
        }

        @Nested
        @DisplayName("구글 계정을 저장하다 같은 계정이 이미 있다고 판정된 경우")
        class WhenGoogleSubjectSaveRejected {

            @Test
            @DisplayName("예외를 던지고 개인 링크 토큰을 발급하거나 저장하지 않는다")
            void shouldThrowParticipantAlreadyJoinedExceptionWithoutLinkToken() {
                // given
                givenJoinableGathering(List.of());
                given(gatheringRepository.save(any(Gathering.class)))
                        .willReturn(
                                GatheringFixture.FRIDAY_DINNER.createWithIdAndOpenedAt(
                                        GATHERING_ID,
                                        OPEN_PERIOD_OPENED_AT,
                                        List.of(
                                                ParticipantFixture.KIM.createWithId(
                                                        PARTICIPANT_ID))));
                willThrow(new ParticipantAlreadyJoinedException())
                        .given(participantGoogleSubjectRepository)
                        .save(GATHERING_ID, PARTICIPANT_ID, GOOGLE_SUBJECT);
                AddParticipantCommand command = command(GatheringPolicy.CONSENT_VERSION);

                // when & then
                assertThatThrownBy(() -> joinGatheringCommandService.add(command))
                        .isInstanceOf(ParticipantAlreadyJoinedException.class);
                then(gatheringTokenProvider).shouldHaveNoInteractions();
                then(participantLinkTokenRepository).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("휴대폰 형식이 틀린 경우")
        class WhenPhoneNumberInvalid {

            @Test
            @DisplayName("예외를 던지고 참여자와 개인 링크 토큰을 저장하지 않는다")
            void shouldThrowInvalidPhoneNumberException() {
                // given
                givenJoinableGathering(List.of());
                AddParticipantCommand command =
                        new AddParticipantCommand(
                                JOIN_TOKEN,
                                GOOGLE_SUBJECT,
                                ParticipantFixture.KIM.getName(),
                                "02-123-4567",
                                true,
                                GatheringPolicy.CONSENT_VERSION);

                // when & then
                assertThatThrownBy(() -> joinGatheringCommandService.add(command))
                        .isInstanceOf(InvalidPhoneNumberException.class);
                thenNothingSaved();
            }
        }

        @Nested
        @DisplayName("010이 아닌 휴대폰으로 참여하는 경우")
        class WhenPhoneNumberNotStartingWith010 {

            @Test
            @DisplayName("예외를 던지고 참여자와 개인 링크 토큰을 저장하지 않는다")
            void shouldThrowInvalidPhoneNumberException() {
                // given
                givenJoinableGathering(List.of());
                AddParticipantCommand command =
                        new AddParticipantCommand(
                                JOIN_TOKEN,
                                GOOGLE_SUBJECT,
                                ParticipantFixture.KIM.getName(),
                                "011-234-5678",
                                true,
                                GatheringPolicy.CONSENT_VERSION);

                // when & then
                assertThatThrownBy(() -> joinGatheringCommandService.add(command))
                        .isInstanceOf(InvalidPhoneNumberException.class);
                thenNothingSaved();
            }
        }

        @Nested
        @DisplayName("이름 앞뒤에 공백이 있는 경우")
        class WhenNameHasSurroundingWhitespace {

            @Test
            @DisplayName("공백을 제거한 이름으로 참여자를 저장한다")
            void shouldSaveStrippedName() {
                // given
                givenJoinableGathering(List.of());
                given(gatheringRepository.save(any(Gathering.class)))
                        .willReturn(
                                GatheringFixture.FRIDAY_DINNER.createWithIdAndOpenedAt(
                                        GATHERING_ID,
                                        OPEN_PERIOD_OPENED_AT,
                                        List.of(
                                                ParticipantFixture.KIM.createWithId(
                                                        PARTICIPANT_ID))));
                given(gatheringTokenProvider.generate()).willReturn(LINK_TOKEN);
                AddParticipantCommand command =
                        new AddParticipantCommand(
                                JOIN_TOKEN,
                                GOOGLE_SUBJECT,
                                "  " + ParticipantFixture.KIM.getName() + "\t ",
                                HYPHENATED_PHONE_NUMBER,
                                true,
                                GatheringPolicy.CONSENT_VERSION);

                // when
                joinGatheringCommandService.add(command);

                // then
                ArgumentCaptor<Gathering> captor = ArgumentCaptor.forClass(Gathering.class);
                then(gatheringRepository).should().save(captor.capture());
                assertThat(captor.getValue().getParticipants().getFirst().getName())
                        .isEqualTo(ParticipantFixture.KIM.getName());
            }
        }

        @Nested
        @DisplayName("참여자가 19명이라 20번째로 참여하는 경우")
        class WhenParticipantCountJustBelowLimit {

            @Test
            @DisplayName("기존 19명을 유지하고 요청한 휴대폰의 참여자를 더해 20명으로 저장한다")
            void shouldSaveParticipant() {
                // given
                givenJoinableGathering(participantsOf(GatheringPolicy.MAX_PARTICIPANT_COUNT - 1));
                given(gatheringRepository.save(any(Gathering.class)))
                        .willReturn(
                                GatheringFixture.FRIDAY_DINNER.createWithIdAndOpenedAt(
                                        GATHERING_ID,
                                        OPEN_PERIOD_OPENED_AT,
                                        List.of(
                                                ParticipantFixture.KIM.createWithId(
                                                        PARTICIPANT_ID))));
                given(gatheringTokenProvider.generate()).willReturn(LINK_TOKEN);
                AddParticipantCommand command = command(GatheringPolicy.CONSENT_VERSION);

                // when
                joinGatheringCommandService.add(command);

                // then
                ArgumentCaptor<Gathering> captor = ArgumentCaptor.forClass(Gathering.class);
                then(gatheringRepository).should().save(captor.capture());
                List<Participant> saved = captor.getValue().getParticipants();
                assertThat(saved).hasSize(GatheringPolicy.MAX_PARTICIPANT_COUNT);
                assertThat(saved.getLast().getPhoneNumber())
                        .isEqualTo(new PhoneNumber(ParticipantFixture.KIM.getPhoneNumber()));
            }
        }

        @Nested
        @DisplayName("기록 기간을 연장한 모임인 경우")
        class WhenRecordingExtended {

            @Test
            @DisplayName("모임을 만든 지 48시간이 지나도 참여자를 저장한다")
            void shouldSaveParticipantAfterOriginalRecordingPeriod() {
                // given
                givenGathering(
                        GatheringFixture.FRIDAY_DINNER.createExtendedWithIdAndOpenedAt(
                                GATHERING_ID, CLOSED_PERIOD_OPENED_AT));
                given(gatheringRepository.save(any(Gathering.class)))
                        .willReturn(
                                GatheringFixture.FRIDAY_DINNER.createWithIdAndOpenedAt(
                                        GATHERING_ID,
                                        OPEN_PERIOD_OPENED_AT,
                                        List.of(
                                                ParticipantFixture.KIM.createWithId(
                                                        PARTICIPANT_ID))));
                given(gatheringTokenProvider.generate()).willReturn(LINK_TOKEN);

                // when
                ParticipantResult result =
                        joinGatheringCommandService.add(command(GatheringPolicy.CONSENT_VERSION));

                // then
                then(gatheringRepository).should().save(any(Gathering.class));
                assertThat(result.linkToken()).isEqualTo(LINK_TOKEN);
            }
        }

        @Nested
        @DisplayName("참여할 수 있는 경우")
        class WhenJoinable {

            @Test
            @DisplayName("정규화한 휴대폰으로 참여자를 더해 저장하고, 저장된 참여자 id로 개인 링크 토큰을 저장한다")
            void shouldSaveParticipantAndLinkTokenWithSavedParticipantId() {
                // given
                givenJoinableGathering(List.of());
                given(gatheringRepository.save(any(Gathering.class)))
                        .willReturn(
                                GatheringFixture.FRIDAY_DINNER.createWithIdAndOpenedAt(
                                        GATHERING_ID,
                                        OPEN_PERIOD_OPENED_AT,
                                        List.of(
                                                ParticipantFixture.KIM.createWithId(
                                                        PARTICIPANT_ID))));
                given(gatheringTokenProvider.generate()).willReturn(LINK_TOKEN);
                AddParticipantCommand command = command(GatheringPolicy.CONSENT_VERSION);

                // when
                ParticipantResult result = joinGatheringCommandService.add(command);

                // then
                ArgumentCaptor<Gathering> captor = ArgumentCaptor.forClass(Gathering.class);
                then(gatheringRepository).should().save(captor.capture());
                Participant joined = captor.getValue().getParticipants().getFirst();
                assertThat(joined.getPhoneNumber())
                        .isEqualTo(new PhoneNumber(ParticipantFixture.KIM.getPhoneNumber()));
                assertThat(joined.getConsentVersion()).isEqualTo(GatheringPolicy.CONSENT_VERSION);
                then(participantLinkTokenRepository).should().save(LINK_TOKEN, PARTICIPANT_ID);
                then(participantGoogleSubjectRepository)
                        .should()
                        .save(GATHERING_ID, PARTICIPANT_ID, GOOGLE_SUBJECT);
                assertThat(result.id()).isEqualTo(PARTICIPANT_ID);
                assertThat(result.name()).isEqualTo(ParticipantFixture.KIM.getName());
                assertThat(result.maskedPhoneNumber()).isEqualTo(MASKED_PHONE_NUMBER);
                assertThat(result.linkToken()).isEqualTo(LINK_TOKEN);
            }
        }

        // 참여하려는 김철수의 휴대폰과 겹치지 않도록 모두 이영희로 만든다. id만 서로 다르다.
        private List<Participant> participantsOf(int count) {
            return LongStream.rangeClosed(1, count)
                    .mapToObj(ParticipantFixture.LEE::createWithId)
                    .toList();
        }

        private AddParticipantCommand command(String consentVersion) {
            return command(true, consentVersion);
        }

        private AddParticipantCommand command(boolean consentAgreed, String consentVersion) {
            return new AddParticipantCommand(
                    JOIN_TOKEN,
                    GOOGLE_SUBJECT,
                    ParticipantFixture.KIM.getName(),
                    HYPHENATED_PHONE_NUMBER,
                    consentAgreed,
                    consentVersion);
        }

        private void givenJoinableGathering(List<Participant> participants) {
            givenGathering(
                    GatheringFixture.FRIDAY_DINNER.createWithIdAndOpenedAt(
                            GATHERING_ID, OPEN_PERIOD_OPENED_AT, participants));
        }

        private void givenGathering(Gathering gathering) {
            given(gatheringJoinTokenRepository.findGatheringIdByValue(JOIN_TOKEN))
                    .willReturn(Optional.of(GATHERING_ID));
            givenRetention(GatheringRetentionFixture.RETAINED);
            given(gatheringRepository.findByIdForUpdate(GATHERING_ID))
                    .willReturn(Optional.of(gathering));
        }

        private void thenNothingSaved() {
            then(gatheringRepository).should(never()).save(any(Gathering.class));
            then(participantLinkTokenRepository).should(never()).save(anyString(), anyLong());
            then(participantGoogleSubjectRepository)
                    .should(never())
                    .save(anyLong(), anyLong(), anyString());
        }
    }

    @Nested
    @DisplayName("joinAsHost 메서드는")
    class JoinAsHost {

        @Nested
        @DisplayName("모임이 존재하지 않는 경우")
        class WhenGatheringNotFound {

            @Test
            @DisplayName("예외를 던지고 아무것도 저장하지 않는다")
            void shouldThrowGatheringNotFoundException() {
                // given
                given(gatheringRetentionRepository.findByGatheringId(GATHERING_ID))
                        .willReturn(Optional.empty());
                JoinGatheringAsHostCommand command = hostCommand(GatheringPolicy.CONSENT_VERSION);

                // when & then
                assertThatThrownBy(() -> joinGatheringCommandService.joinAsHost(command))
                        .isInstanceOf(GatheringNotFoundException.class);
                thenNothingSaved();
            }
        }

        @Nested
        @DisplayName("모임의 파기 시각이 지난 경우")
        class WhenGatheringExpired {

            @Test
            @DisplayName("물리 삭제 전이라도 모임을 찾지 못한 예외를 던지고 모임을 읽지 않는다")
            void shouldThrowGatheringNotFoundException() {
                // given
                givenRetention(GatheringRetentionFixture.EXPIRED);
                JoinGatheringAsHostCommand command = hostCommand(GatheringPolicy.CONSENT_VERSION);

                // when & then
                assertThatThrownBy(() -> joinGatheringCommandService.joinAsHost(command))
                        .isInstanceOf(GatheringNotFoundException.class);
                then(gatheringRepository).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("모임을 만든 회원이 아닌 경우")
        class WhenMemberIsNotHost {

            @Test
            @DisplayName("예외를 던지고 아무것도 저장하지 않는다")
            void shouldThrowNotGatheringHostException() {
                // given
                givenHostGathering(OPEN_PERIOD_OPENED_AT, List.of());
                JoinGatheringAsHostCommand command =
                        new JoinGatheringAsHostCommand(
                                OTHER_MEMBER_ID,
                                GATHERING_ID,
                                ParticipantFixture.KIM.getName(),
                                HYPHENATED_PHONE_NUMBER,
                                true,
                                GatheringPolicy.CONSENT_VERSION);

                // when & then
                assertThatThrownBy(() -> joinGatheringCommandService.joinAsHost(command))
                        .isInstanceOf(NotGatheringHostException.class);
                thenNothingSaved();
            }
        }

        @Nested
        @DisplayName("모임을 만든 지 48시간이 된 경우")
        class WhenRecordingPeriodClosed {

            @Test
            @DisplayName("예외를 던지고 아무것도 저장하지 않는다")
            void shouldThrowGatheringJoinClosedException() {
                // given
                givenHostGathering(CLOSED_PERIOD_OPENED_AT, List.of());
                JoinGatheringAsHostCommand command = hostCommand(GatheringPolicy.CONSENT_VERSION);

                // when & then
                assertThatThrownBy(() -> joinGatheringCommandService.joinAsHost(command))
                        .isInstanceOf(GatheringJoinClosedException.class);
                thenNothingSaved();
            }
        }

        @Nested
        @DisplayName("동의하지 않은 경우")
        class WhenConsentNotAgreed {

            @Test
            @DisplayName("예외를 던지고 아무것도 저장하지 않는다")
            void shouldThrowConsentRequiredException() {
                // given
                givenHostGathering(OPEN_PERIOD_OPENED_AT, List.of());
                JoinGatheringAsHostCommand command =
                        new JoinGatheringAsHostCommand(
                                GatheringFixture.FRIDAY_DINNER.getHostMemberId(),
                                GATHERING_ID,
                                ParticipantFixture.KIM.getName(),
                                HYPHENATED_PHONE_NUMBER,
                                false,
                                GatheringPolicy.CONSENT_VERSION);

                // when & then
                assertThatThrownBy(() -> joinGatheringCommandService.joinAsHost(command))
                        .isInstanceOf(ConsentRequiredException.class);
                thenNothingSaved();
            }
        }

        @Nested
        @DisplayName("동의 안내문 버전이 다른 경우")
        class WhenConsentVersionOutdated {

            @Test
            @DisplayName("예외를 던지고 아무것도 저장하지 않는다")
            void shouldThrowOutdatedConsentVersionException() {
                // given
                givenHostGathering(OPEN_PERIOD_OPENED_AT, List.of());
                JoinGatheringAsHostCommand command = hostCommand("2000-01-01");

                // when & then
                assertThatThrownBy(() -> joinGatheringCommandService.joinAsHost(command))
                        .isInstanceOf(OutdatedConsentVersionException.class);
                thenNothingSaved();
            }
        }

        @Nested
        @DisplayName("이미 참여한 휴대폰인 경우")
        class WhenPhoneNumberAlreadyJoined {

            @Test
            @DisplayName("예외를 던지고 아무것도 저장하지 않는다")
            void shouldThrowParticipantAlreadyJoinedException() {
                // given
                givenHostGathering(
                        OPEN_PERIOD_OPENED_AT,
                        List.of(ParticipantFixture.KIM.createWithId(PARTICIPANT_ID)));
                JoinGatheringAsHostCommand command = hostCommand(GatheringPolicy.CONSENT_VERSION);

                // when & then
                assertThatThrownBy(() -> joinGatheringCommandService.joinAsHost(command))
                        .isInstanceOf(ParticipantAlreadyJoinedException.class);
                thenNothingSaved();
            }
        }

        @Nested
        @DisplayName("참여자가 이미 20명인 경우")
        class WhenParticipantCountAtLimit {

            @Test
            @DisplayName("예외를 던지고 아무것도 저장하지 않는다")
            void shouldThrowGatheringParticipantCountExceedsLimitException() {
                // given
                givenHostGathering(
                        OPEN_PERIOD_OPENED_AT,
                        LongStream.rangeClosed(1, GatheringPolicy.MAX_PARTICIPANT_COUNT)
                                .mapToObj(ParticipantFixture.LEE::createWithId)
                                .toList());
                JoinGatheringAsHostCommand command = hostCommand(GatheringPolicy.CONSENT_VERSION);

                // when & then
                assertThatThrownBy(() -> joinGatheringCommandService.joinAsHost(command))
                        .isInstanceOf(GatheringParticipantCountExceedsLimitException.class);
                thenNothingSaved();
            }
        }

        @Nested
        @DisplayName("모임장이 참여할 수 있는 경우")
        class WhenJoinable {

            @Test
            @DisplayName("정규화한 휴대폰으로 참여자를 더해 저장하고, 저장된 참여자 id로 개인 링크 토큰을 저장해 반환한다")
            void shouldSaveParticipantAndLinkTokenWithSavedParticipantId() {
                // given
                givenHostGathering(OPEN_PERIOD_OPENED_AT, List.of());
                given(gatheringRepository.save(any(Gathering.class)))
                        .willReturn(
                                GatheringFixture.FRIDAY_DINNER.createWithIdAndOpenedAt(
                                        GATHERING_ID,
                                        OPEN_PERIOD_OPENED_AT,
                                        List.of(
                                                ParticipantFixture.KIM.createWithId(
                                                        PARTICIPANT_ID))));
                given(gatheringTokenProvider.generate()).willReturn(LINK_TOKEN);
                JoinGatheringAsHostCommand command = hostCommand(GatheringPolicy.CONSENT_VERSION);

                // when
                ParticipantResult result = joinGatheringCommandService.joinAsHost(command);

                // then
                ArgumentCaptor<Gathering> captor = ArgumentCaptor.forClass(Gathering.class);
                then(gatheringRepository).should().save(captor.capture());
                Participant joined = captor.getValue().getParticipants().getFirst();
                assertThat(joined.getPhoneNumber())
                        .isEqualTo(new PhoneNumber(ParticipantFixture.KIM.getPhoneNumber()));
                assertThat(joined.getConsentVersion()).isEqualTo(GatheringPolicy.CONSENT_VERSION);
                then(participantLinkTokenRepository).should().save(LINK_TOKEN, PARTICIPANT_ID);
                then(participantGoogleSubjectRepository).shouldHaveNoInteractions();
                assertThat(result.id()).isEqualTo(PARTICIPANT_ID);
                assertThat(result.maskedPhoneNumber()).isEqualTo(MASKED_PHONE_NUMBER);
                assertThat(result.linkToken()).isEqualTo(LINK_TOKEN);
            }
        }

        private JoinGatheringAsHostCommand hostCommand(String consentVersion) {
            return new JoinGatheringAsHostCommand(
                    GatheringFixture.FRIDAY_DINNER.getHostMemberId(),
                    GATHERING_ID,
                    ParticipantFixture.KIM.getName(),
                    HYPHENATED_PHONE_NUMBER,
                    true,
                    consentVersion);
        }

        private void givenHostGathering(LocalDateTime openedAt, List<Participant> participants) {
            givenRetention(GatheringRetentionFixture.RETAINED);
            given(gatheringRepository.findByIdForUpdate(GATHERING_ID))
                    .willReturn(
                            Optional.of(
                                    GatheringFixture.FRIDAY_DINNER.createWithIdAndOpenedAt(
                                            GATHERING_ID, openedAt, participants)));
        }

        private void thenNothingSaved() {
            then(gatheringRepository).should(never()).save(any(Gathering.class));
            then(participantLinkTokenRepository).should(never()).save(anyString(), anyLong());
            then(participantGoogleSubjectRepository).shouldHaveNoInteractions();
        }
    }

    private void givenRetention(GatheringRetentionFixture retention) {
        given(gatheringRetentionRepository.findByGatheringId(GATHERING_ID))
                .willReturn(Optional.of(retention.create(GATHERING_ID, NOW)));
    }
}
