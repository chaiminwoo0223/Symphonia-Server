package com.symphonia.gathering.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;

import com.symphonia.UnitTest;
import com.symphonia.gathering.application.dto.command.CorrectParticipantByGoogleSubjectCommand;
import com.symphonia.gathering.application.dto.command.ReissueParticipantLinkTokenByGoogleSubjectCommand;
import com.symphonia.gathering.application.dto.command.ReissueParticipantLinkTokenCommand;
import com.symphonia.gathering.application.dto.command.RemoveParticipantBySubjectCommand;
import com.symphonia.gathering.application.dto.command.RemoveParticipantCommand;
import com.symphonia.gathering.application.dto.result.ParticipantResult;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.entity.Participant;
import com.symphonia.gathering.domain.exception.GatheringNotFoundException;
import com.symphonia.gathering.domain.exception.InvalidPhoneNumberException;
import com.symphonia.gathering.domain.exception.JoinTokenNotFoundException;
import com.symphonia.gathering.domain.exception.NotGatheringHostException;
import com.symphonia.gathering.domain.exception.ParticipantAlreadyJoinedException;
import com.symphonia.gathering.domain.exception.ParticipantNotFoundException;
import com.symphonia.gathering.domain.exception.ParticipantUnremovableException;
import com.symphonia.gathering.domain.provider.GatheringTokenProvider;
import com.symphonia.gathering.domain.repository.GatheringJoinTokenRepository;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.domain.repository.GatheringRetentionRepository;
import com.symphonia.gathering.domain.repository.ParticipantGoogleSubjectRepository;
import com.symphonia.gathering.domain.repository.ParticipantLinkTokenRepository;
import com.symphonia.gathering.domain.repository.ParticipantPairingFeedbackRepository;
import com.symphonia.gathering.domain.repository.SettlementRepository;
import com.symphonia.gathering.fixture.GatheringFixture;
import com.symphonia.gathering.fixture.GatheringRetentionFixture;
import com.symphonia.gathering.fixture.ParticipantFixture;
import com.symphonia.global.config.TimeConfig;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;

@DisplayName("ParticipantCommandService 단위 테스트")
class ParticipantCommandServiceTest extends UnitTest {

    private static final Long GATHERING_ID = 10L;
    private static final Long PARTICIPANT_ID = 20L;
    private static final Long OTHER_PARTICIPANT_ID = 21L;
    private static final Long OTHER_MEMBER_ID = 99L;
    private static final Long HOST_MEMBER_ID = GatheringFixture.FRIDAY_DINNER.getHostMemberId();
    private static final String JOIN_TOKEN = "join-token";
    private static final String GOOGLE_SUBJECT = "109876543210987654321";
    private static final String NEW_LINK_TOKEN = "new-link-token";
    private static final String MASKED_PHONE_NUMBER = "010-****-5678";
    private static final String NEW_PHONE_NUMBER = "01077778888";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 19, 18, 0);

    // 기록 기간(48시간)이 지나도 링크를 잃은 참여자는 분담금을 보내야 하므로, 닫힌 뒤의 모임으로 경계를 확인한다.
    private static final LocalDateTime OPEN_PERIOD_OPENED_AT = NOW.minusHours(48).plusSeconds(1);
    private static final LocalDateTime CLOSED_PERIOD_OPENED_AT = NOW.minusHours(48);

    private ParticipantCommandService participantCommandService;

    @Mock private GatheringRepository gatheringRepository;
    @Mock private GatheringJoinTokenRepository gatheringJoinTokenRepository;
    @Mock private ParticipantGoogleSubjectRepository participantGoogleSubjectRepository;
    @Mock private GatheringRetentionRepository gatheringRetentionRepository;
    @Mock private ParticipantLinkTokenRepository participantLinkTokenRepository;
    @Mock private ParticipantPairingFeedbackRepository participantPairingFeedbackRepository;
    @Mock private SettlementRepository settlementRepository;
    @Mock private GatheringTokenProvider gatheringTokenProvider;

    @BeforeEach
    void setUp() {
        participantCommandService =
                new ParticipantCommandService(
                        gatheringRepository,
                        gatheringJoinTokenRepository,
                        participantGoogleSubjectRepository,
                        gatheringRetentionRepository,
                        participantLinkTokenRepository,
                        participantPairingFeedbackRepository,
                        settlementRepository,
                        gatheringTokenProvider,
                        Clock.fixed(
                                NOW.atZone(TimeConfig.SERVICE_ZONE).toInstant(),
                                TimeConfig.SERVICE_ZONE));
    }

    @Nested
    @DisplayName("reissue 메서드는")
    class Reissue {

        @Nested
        @DisplayName("모임이 존재하지 않는 경우")
        class WhenGatheringNotFound {

            @Test
            @DisplayName("예외를 던지고 개인 링크 토큰을 지우거나 저장하지 않는다")
            void shouldThrowGatheringNotFoundException() {
                // given
                given(gatheringRetentionRepository.findByGatheringId(GATHERING_ID))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> participantCommandService.reissue(command(HOST_MEMBER_ID)))
                        .isInstanceOf(GatheringNotFoundException.class);
                then(participantLinkTokenRepository).shouldHaveNoInteractions();
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

                // when & then
                assertThatThrownBy(() -> participantCommandService.reissue(command(HOST_MEMBER_ID)))
                        .isInstanceOf(GatheringNotFoundException.class);
                then(gatheringRepository).shouldHaveNoInteractions();
                then(participantLinkTokenRepository).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("모임을 만든 회원이 아닌 경우")
        class WhenMemberIsNotHost {

            @Test
            @DisplayName("예외를 던지고 개인 링크 토큰을 지우거나 저장하지 않는다")
            void shouldThrowNotGatheringHostException() {
                // given
                givenGatheringWithParticipant(OPEN_PERIOD_OPENED_AT);

                // when & then
                assertThatThrownBy(
                                () -> participantCommandService.reissue(command(OTHER_MEMBER_ID)))
                        .isInstanceOf(NotGatheringHostException.class);
                then(gatheringTokenProvider).shouldHaveNoInteractions();
                then(participantLinkTokenRepository).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("모임에 없는 참여자인 경우")
        class WhenParticipantNotFound {

            @Test
            @DisplayName("예외를 던지고 개인 링크 토큰을 지우거나 저장하지 않는다")
            void shouldThrowParticipantNotFoundException() {
                // given
                givenGatheringWithParticipant(OPEN_PERIOD_OPENED_AT);
                ReissueParticipantLinkTokenCommand command =
                        ReissueParticipantLinkTokenCommand.of(
                                HOST_MEMBER_ID, GATHERING_ID, OTHER_PARTICIPANT_ID);

                // when & then
                assertThatThrownBy(() -> participantCommandService.reissue(command))
                        .isInstanceOf(ParticipantNotFoundException.class);
                then(gatheringTokenProvider).shouldHaveNoInteractions();
                then(participantLinkTokenRepository).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("모임을 만든 회원의 요청인 경우")
        class WhenMemberIsHost {

            @Test
            @DisplayName("기존 개인 링크 토큰을 모두 지운 뒤 새 토큰을 저장하고 반환한다")
            void shouldReplaceLinkTokens() {
                // given
                givenGatheringWithParticipant(OPEN_PERIOD_OPENED_AT);
                given(gatheringTokenProvider.generate()).willReturn(NEW_LINK_TOKEN);

                // when
                ParticipantResult result =
                        participantCommandService.reissue(command(HOST_MEMBER_ID));

                // then
                InOrder inOrder = inOrder(participantLinkTokenRepository);
                then(participantLinkTokenRepository)
                        .should(inOrder)
                        .deleteAllByParticipantId(PARTICIPANT_ID);
                then(participantLinkTokenRepository)
                        .should(inOrder)
                        .save(NEW_LINK_TOKEN, PARTICIPANT_ID);
                assertThat(result.id()).isEqualTo(PARTICIPANT_ID);
                assertThat(result.name()).isEqualTo(ParticipantFixture.KIM.getName());
                assertThat(result.maskedPhoneNumber()).isEqualTo(MASKED_PHONE_NUMBER);
                assertThat(result.linkToken()).isEqualTo(NEW_LINK_TOKEN);
            }
        }

        @Nested
        @DisplayName("모임을 만든 지 48시간이 된 경우")
        class WhenRecordingPeriodClosed {

            @Test
            @DisplayName("기록 기간과 관계없이 새 개인 링크 토큰을 저장하고 반환한다")
            void shouldReplaceLinkTokens() {
                // given
                givenGatheringWithParticipant(CLOSED_PERIOD_OPENED_AT);
                given(gatheringTokenProvider.generate()).willReturn(NEW_LINK_TOKEN);

                // when
                ParticipantResult result =
                        participantCommandService.reissue(command(HOST_MEMBER_ID));

                // then
                then(participantLinkTokenRepository).should().save(NEW_LINK_TOKEN, PARTICIPANT_ID);
                assertThat(result.linkToken()).isEqualTo(NEW_LINK_TOKEN);
            }
        }

        private ReissueParticipantLinkTokenCommand command(Long memberId) {
            return ReissueParticipantLinkTokenCommand.of(memberId, GATHERING_ID, PARTICIPANT_ID);
        }
    }

    @Nested
    @DisplayName("reissueByGoogleSubject 메서드는")
    class ReissueByGoogleSubject {

        @Nested
        @DisplayName("참여 토큰이 가리키는 모임이 없는 경우")
        class WhenJoinTokenNotFound {

            @Test
            @DisplayName("예외를 던지고 모임을 읽거나 개인 링크 토큰을 지우거나 저장하지 않는다")
            void shouldThrowJoinTokenNotFoundException() {
                // given
                given(gatheringJoinTokenRepository.findGatheringIdByValue(JOIN_TOKEN))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(
                                () -> participantCommandService.reissueByGoogleSubject(command()))
                        .isInstanceOf(JoinTokenNotFoundException.class);
                then(gatheringRepository).shouldHaveNoInteractions();
                then(participantLinkTokenRepository).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("모임의 파기 시각이 지난 경우")
        class WhenGatheringExpired {

            @Test
            @DisplayName("물리 삭제 전이라도 참여 토큰을 찾지 못한 예외를 던지고 모임을 읽지 않는다")
            void shouldThrowJoinTokenNotFoundException() {
                // given
                givenJoinToken();
                givenRetention(GatheringRetentionFixture.EXPIRED);

                // when & then
                assertThatThrownBy(
                                () -> participantCommandService.reissueByGoogleSubject(command()))
                        .isInstanceOf(JoinTokenNotFoundException.class);
                then(gatheringRepository).shouldHaveNoInteractions();
                then(participantLinkTokenRepository).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("모임의 파기 시각 1초 전인 경우")
        class WhenGatheringAboutToExpire {

            @Test
            @DisplayName("새 개인 링크 토큰을 저장하고 반환한다")
            void shouldReplaceLinkTokens() {
                // given
                givenJoinToken();
                givenRetention(GatheringRetentionFixture.ABOUT_TO_EXPIRE);
                givenLockedGathering(OPEN_PERIOD_OPENED_AT);
                givenParticipantOfGoogleSubject();
                given(gatheringTokenProvider.generate()).willReturn(NEW_LINK_TOKEN);

                // when
                ParticipantResult result =
                        participantCommandService.reissueByGoogleSubject(command());

                // then
                then(participantLinkTokenRepository).should().save(NEW_LINK_TOKEN, PARTICIPANT_ID);
                assertThat(result.linkToken()).isEqualTo(NEW_LINK_TOKEN);
            }
        }

        @Nested
        @DisplayName("같은 구글 계정으로 참여한 참여자가 없는 경우")
        class WhenParticipantNotFound {

            @Test
            @DisplayName("예외를 던지고 개인 링크 토큰을 지우거나 저장하지 않는다")
            void shouldThrowParticipantNotFoundException() {
                // given
                givenJoinToken();
                givenGatheringWithParticipant(OPEN_PERIOD_OPENED_AT);
                given(
                                participantGoogleSubjectRepository.findParticipantId(
                                        GATHERING_ID, GOOGLE_SUBJECT))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(
                                () -> participantCommandService.reissueByGoogleSubject(command()))
                        .isInstanceOf(ParticipantNotFoundException.class);
                then(gatheringTokenProvider).shouldHaveNoInteractions();
                then(participantLinkTokenRepository).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("같은 구글 계정으로 참여한 참여자가 있는 경우")
        class WhenParticipantExists {

            @Test
            @DisplayName("기존 개인 링크 토큰을 모두 지운 뒤 새 토큰을 저장하고 반환한다")
            void shouldReplaceLinkTokens() {
                // given
                givenJoinToken();
                givenGatheringWithParticipant(OPEN_PERIOD_OPENED_AT);
                givenParticipantOfGoogleSubject();
                given(gatheringTokenProvider.generate()).willReturn(NEW_LINK_TOKEN);

                // when
                ParticipantResult result =
                        participantCommandService.reissueByGoogleSubject(command());

                // then
                InOrder inOrder = inOrder(participantLinkTokenRepository);
                then(participantLinkTokenRepository)
                        .should(inOrder)
                        .deleteAllByParticipantId(PARTICIPANT_ID);
                then(participantLinkTokenRepository)
                        .should(inOrder)
                        .save(NEW_LINK_TOKEN, PARTICIPANT_ID);
                assertThat(result.id()).isEqualTo(PARTICIPANT_ID);
                assertThat(result.name()).isEqualTo(ParticipantFixture.KIM.getName());
                assertThat(result.maskedPhoneNumber()).isEqualTo(MASKED_PHONE_NUMBER);
                assertThat(result.linkToken()).isEqualTo(NEW_LINK_TOKEN);
            }
        }

        @Nested
        @DisplayName("모임을 만든 지 48시간이 된 경우")
        class WhenRecordingPeriodClosed {

            @Test
            @DisplayName("기록 기간과 관계없이 새 개인 링크 토큰을 저장하고 반환한다")
            void shouldReplaceLinkTokens() {
                // given
                givenJoinToken();
                givenGatheringWithParticipant(CLOSED_PERIOD_OPENED_AT);
                givenParticipantOfGoogleSubject();
                given(gatheringTokenProvider.generate()).willReturn(NEW_LINK_TOKEN);

                // when
                ParticipantResult result =
                        participantCommandService.reissueByGoogleSubject(command());

                // then
                then(participantLinkTokenRepository).should().save(NEW_LINK_TOKEN, PARTICIPANT_ID);
                assertThat(result.linkToken()).isEqualTo(NEW_LINK_TOKEN);
            }
        }

        private ReissueParticipantLinkTokenByGoogleSubjectCommand command() {
            return new ReissueParticipantLinkTokenByGoogleSubjectCommand(
                    JOIN_TOKEN, GOOGLE_SUBJECT);
        }

        private void givenJoinToken() {
            given(gatheringJoinTokenRepository.findGatheringIdByValue(JOIN_TOKEN))
                    .willReturn(Optional.of(GATHERING_ID));
        }

        private void givenParticipantOfGoogleSubject() {
            given(
                            participantGoogleSubjectRepository.findParticipantId(
                                    GATHERING_ID, GOOGLE_SUBJECT))
                    .willReturn(Optional.of(PARTICIPANT_ID));
        }
    }

    @Nested
    @DisplayName("correctByGoogleSubject 메서드는")
    class CorrectByGoogleSubject {

        @Nested
        @DisplayName("참여 토큰이 가리키는 모임이 없는 경우")
        class WhenJoinTokenNotFound {

            @Test
            @DisplayName("예외를 던지고 모임을 읽거나 참여자를 바꾸지 않는다")
            void shouldThrowJoinTokenNotFoundException() {
                // given
                given(gatheringJoinTokenRepository.findGatheringIdByValue(JOIN_TOKEN))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(
                                () ->
                                        participantCommandService.correctByGoogleSubject(
                                                correctCommand(NEW_PHONE_NUMBER)))
                        .isInstanceOf(JoinTokenNotFoundException.class);
                then(gatheringRepository).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("모임의 파기 시각이 지난 경우")
        class WhenGatheringExpired {

            @Test
            @DisplayName("물리 삭제 전이라도 참여 토큰을 찾지 못한 예외를 던지고 모임을 읽지 않는다")
            void shouldThrowJoinTokenNotFoundException() {
                // given
                givenJoinToken();
                givenRetention(GatheringRetentionFixture.EXPIRED);

                // when & then
                assertThatThrownBy(
                                () ->
                                        participantCommandService.correctByGoogleSubject(
                                                correctCommand(NEW_PHONE_NUMBER)))
                        .isInstanceOf(JoinTokenNotFoundException.class);
                then(gatheringRepository).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("같은 구글 계정으로 참여한 참여자가 없는 경우")
        class WhenParticipantNotFound {

            @Test
            @DisplayName("예외를 던지고 참여자를 바꾸지 않는다")
            void shouldThrowParticipantNotFoundException() {
                // given
                givenJoinToken();
                givenGatheringWithTwoParticipants(OPEN_PERIOD_OPENED_AT);
                given(
                                participantGoogleSubjectRepository.findParticipantId(
                                        GATHERING_ID, GOOGLE_SUBJECT))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(
                                () ->
                                        participantCommandService.correctByGoogleSubject(
                                                correctCommand(NEW_PHONE_NUMBER)))
                        .isInstanceOf(ParticipantNotFoundException.class);
                then(gatheringRepository)
                        .should(never())
                        .updateParticipant(anyLong(), any(Participant.class));
            }
        }

        @Nested
        @DisplayName("010으로 시작하는 11자리가 아닌 휴대폰인 경우")
        class WhenPhoneNumberInvalid {

            @Test
            @DisplayName("예외를 던지고 참여자를 바꾸지 않는다")
            void shouldThrowInvalidPhoneNumberException() {
                // given
                givenJoinToken();
                givenGatheringWithTwoParticipants(OPEN_PERIOD_OPENED_AT);
                givenParticipantOfGoogleSubject();

                // when & then
                assertThatThrownBy(
                                () ->
                                        participantCommandService.correctByGoogleSubject(
                                                correctCommand("01112345678")))
                        .isInstanceOf(InvalidPhoneNumberException.class);
                then(gatheringRepository)
                        .should(never())
                        .updateParticipant(anyLong(), any(Participant.class));
            }
        }

        @Nested
        @DisplayName("같은 모임의 다른 참여자가 쓰는 휴대폰인 경우")
        class WhenPhoneNumberUsedByOtherParticipant {

            @Test
            @DisplayName("예외를 던지고 참여자를 바꾸지 않는다")
            void shouldThrowParticipantAlreadyJoinedException() {
                // given
                givenJoinToken();
                givenGatheringWithTwoParticipants(OPEN_PERIOD_OPENED_AT);
                givenParticipantOfGoogleSubject();

                // when & then
                assertThatThrownBy(
                                () ->
                                        participantCommandService.correctByGoogleSubject(
                                                correctCommand(
                                                        ParticipantFixture.LEE.getPhoneNumber())))
                        .isInstanceOf(ParticipantAlreadyJoinedException.class);
                then(gatheringRepository)
                        .should(never())
                        .updateParticipant(anyLong(), any(Participant.class));
            }
        }

        @Nested
        @DisplayName("다른 참여자가 쓰지 않는 휴대폰인 경우")
        class WhenPhoneNumberAvailable {

            @Test
            @DisplayName("이름의 앞뒤 공백을 제거하고 하이픈을 지운 휴대폰으로 참여자를 바꾼다")
            void shouldUpdateParticipantWithNormalizedValues() {
                // given
                givenJoinToken();
                givenGatheringWithTwoParticipants(OPEN_PERIOD_OPENED_AT);
                givenParticipantOfGoogleSubject();

                // when
                participantCommandService.correctByGoogleSubject(
                        new CorrectParticipantByGoogleSubjectCommand(
                                JOIN_TOKEN, GOOGLE_SUBJECT, " 김영수 ", "010-7777-8888"));

                // then
                Participant corrected = captureUpdatedParticipant();
                assertThat(corrected.getId()).isEqualTo(PARTICIPANT_ID);
                assertThat(corrected.getName()).isEqualTo("김영수");
                assertThat(corrected.getPhoneNumber().value()).isEqualTo(NEW_PHONE_NUMBER);
            }
        }

        @Nested
        @DisplayName("자기 휴대폰을 그대로 두고 이름만 바꾸는 경우")
        class WhenPhoneNumberUnchanged {

            @Test
            @DisplayName("자기 휴대폰은 중복으로 보지 않고 참여자를 바꾼다")
            void shouldUpdateParticipant() {
                // given
                givenJoinToken();
                givenGatheringWithTwoParticipants(OPEN_PERIOD_OPENED_AT);
                givenParticipantOfGoogleSubject();

                // when
                participantCommandService.correctByGoogleSubject(
                        correctCommand(ParticipantFixture.KIM.getPhoneNumber()));

                // then
                Participant corrected = captureUpdatedParticipant();
                assertThat(corrected.getName()).isEqualTo("김영수");
                assertThat(corrected.getPhoneNumber().value())
                        .isEqualTo(ParticipantFixture.KIM.getPhoneNumber());
            }
        }

        @Nested
        @DisplayName("모임을 만든 지 48시간이 된 경우")
        class WhenRecordingPeriodClosed {

            @Test
            @DisplayName("기록 기간과 관계없이 참여자를 바꾼다")
            void shouldUpdateParticipant() {
                // given
                givenJoinToken();
                givenGatheringWithTwoParticipants(CLOSED_PERIOD_OPENED_AT);
                givenParticipantOfGoogleSubject();

                // when
                participantCommandService.correctByGoogleSubject(correctCommand(NEW_PHONE_NUMBER));

                // then
                assertThat(captureUpdatedParticipant().getPhoneNumber().value())
                        .isEqualTo(NEW_PHONE_NUMBER);
            }
        }

        private CorrectParticipantByGoogleSubjectCommand correctCommand(String phoneNumber) {
            return new CorrectParticipantByGoogleSubjectCommand(
                    JOIN_TOKEN, GOOGLE_SUBJECT, "김영수", phoneNumber);
        }

        private Participant captureUpdatedParticipant() {
            ArgumentCaptor<Participant> captor = ArgumentCaptor.forClass(Participant.class);
            then(gatheringRepository)
                    .should()
                    .updateParticipant(eq(GATHERING_ID), captor.capture());

            return captor.getValue();
        }

        private void givenJoinToken() {
            given(gatheringJoinTokenRepository.findGatheringIdByValue(JOIN_TOKEN))
                    .willReturn(Optional.of(GATHERING_ID));
        }

        private void givenParticipantOfGoogleSubject() {
            given(
                            participantGoogleSubjectRepository.findParticipantId(
                                    GATHERING_ID, GOOGLE_SUBJECT))
                    .willReturn(Optional.of(PARTICIPANT_ID));
        }

        private void givenGatheringWithTwoParticipants(LocalDateTime openedAt) {
            givenRetention(GatheringRetentionFixture.RETAINED);
            given(gatheringRepository.findByIdForUpdate(GATHERING_ID))
                    .willReturn(
                            Optional.of(
                                    GatheringFixture.FRIDAY_DINNER.createWithIdAndOpenedAt(
                                            GATHERING_ID,
                                            openedAt,
                                            List.of(
                                                    ParticipantFixture.KIM.createWithId(
                                                            PARTICIPANT_ID),
                                                    ParticipantFixture.LEE.createWithId(
                                                            OTHER_PARTICIPANT_ID)))));
        }
    }

    @Nested
    @DisplayName("remove 메서드는")
    class Remove {

        @Nested
        @DisplayName("모임이 존재하지 않는 경우")
        class WhenGatheringNotFound {

            @Test
            @DisplayName("예외를 던지고 아무것도 지우지 않는다")
            void shouldThrowGatheringNotFoundException() {
                // given
                given(gatheringRetentionRepository.findByGatheringId(GATHERING_ID))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> participantCommandService.remove(command(HOST_MEMBER_ID)))
                        .isInstanceOf(GatheringNotFoundException.class);
                thenNothingDeleted();
            }
        }

        @Nested
        @DisplayName("모임의 파기 시각이 지난 경우")
        class WhenGatheringExpired {

            @Test
            @DisplayName("물리 삭제 전이라도 모임을 찾지 못한 예외를 던지고 아무것도 지우지 않는다")
            void shouldThrowGatheringNotFoundException() {
                // given
                givenRetention(GatheringRetentionFixture.EXPIRED);

                // when & then
                assertThatThrownBy(() -> participantCommandService.remove(command(HOST_MEMBER_ID)))
                        .isInstanceOf(GatheringNotFoundException.class);
                then(gatheringRepository).shouldHaveNoInteractions();
                thenNothingDeleted();
            }
        }

        @Nested
        @DisplayName("모임을 만든 회원이 아닌 경우")
        class WhenMemberIsNotHost {

            @Test
            @DisplayName("예외를 던지고 아무것도 지우지 않는다")
            void shouldThrowNotGatheringHostException() {
                // given
                givenGatheringWithParticipant(OPEN_PERIOD_OPENED_AT);

                // when & then
                assertThatThrownBy(() -> participantCommandService.remove(command(OTHER_MEMBER_ID)))
                        .isInstanceOf(NotGatheringHostException.class);
                thenNothingDeleted();
            }
        }

        @Nested
        @DisplayName("모임에 없는 참여자인 경우")
        class WhenParticipantNotFound {

            @Test
            @DisplayName("예외를 던지고 아무것도 지우지 않는다")
            void shouldThrowParticipantNotFoundException() {
                // given
                givenGatheringWithParticipant(OPEN_PERIOD_OPENED_AT);
                RemoveParticipantCommand command =
                        RemoveParticipantCommand.of(
                                HOST_MEMBER_ID, GATHERING_ID, OTHER_PARTICIPANT_ID);

                // when & then
                assertThatThrownBy(() -> participantCommandService.remove(command))
                        .isInstanceOf(ParticipantNotFoundException.class);
                thenNothingDeleted();
            }
        }

        @Nested
        @DisplayName("취소하지 않은 정산 건의 결제자이거나 인원인 참여자인 경우")
        class WhenParticipantInUncancelledSettlement {

            @Test
            @DisplayName("예외를 던지고 아무것도 지우거나 익명화하지 않는다")
            void shouldThrowParticipantUnremovableException() {
                // given
                givenGatheringWithParticipant(OPEN_PERIOD_OPENED_AT);
                given(settlementRepository.existsUncancelledByParticipantId(PARTICIPANT_ID))
                        .willReturn(true);

                // when & then
                assertThatThrownBy(() -> participantCommandService.remove(command(HOST_MEMBER_ID)))
                        .isInstanceOf(ParticipantUnremovableException.class);
                thenNothingDeleted();
            }
        }

        @Nested
        @DisplayName("취소한 정산 건에만 든 참여자인 경우")
        class WhenParticipantOnlyInCancelledSettlement {

            @Test
            @DisplayName("개인 링크 토큰과 피드백 제출 표시를 지운 뒤 참여자를 익명화하고 행은 지우지 않는다")
            void shouldAnonymizeParticipant() {
                // given
                givenGatheringWithParticipant(OPEN_PERIOD_OPENED_AT);
                given(settlementRepository.existsByParticipantId(PARTICIPANT_ID)).willReturn(true);

                // when
                participantCommandService.remove(command(HOST_MEMBER_ID));

                // then
                InOrder inOrder =
                        inOrder(
                                participantLinkTokenRepository,
                                participantPairingFeedbackRepository,
                                gatheringRepository);
                then(participantLinkTokenRepository)
                        .should(inOrder)
                        .deleteAllByParticipantId(PARTICIPANT_ID);
                then(participantPairingFeedbackRepository)
                        .should(inOrder)
                        .deleteByParticipantId(PARTICIPANT_ID);
                then(gatheringRepository).should(inOrder).anonymizeParticipant(PARTICIPANT_ID, NOW);
                then(gatheringRepository).should(never()).deleteParticipant(anyLong());
            }
        }

        @Nested
        @DisplayName("정산 건에 전혀 없는 참여자인 경우")
        class WhenParticipantNotInAnySettlement {

            @Test
            @DisplayName("개인 링크 토큰, 피드백 제출 표시, 참여자 순서로 지우고 익명화하지 않는다")
            void shouldDeleteReferencesBeforeParticipant() {
                // given
                givenGatheringWithParticipant(OPEN_PERIOD_OPENED_AT);

                // when
                participantCommandService.remove(command(HOST_MEMBER_ID));

                // then
                InOrder inOrder =
                        inOrder(
                                participantLinkTokenRepository,
                                participantPairingFeedbackRepository,
                                gatheringRepository);
                then(participantLinkTokenRepository)
                        .should(inOrder)
                        .deleteAllByParticipantId(PARTICIPANT_ID);
                then(participantPairingFeedbackRepository)
                        .should(inOrder)
                        .deleteByParticipantId(PARTICIPANT_ID);
                then(gatheringRepository).should(inOrder).deleteParticipant(PARTICIPANT_ID);
                then(gatheringRepository)
                        .should(never())
                        .anonymizeParticipant(anyLong(), any(LocalDateTime.class));
            }
        }

        @Nested
        @DisplayName("모임을 만든 지 48시간이 된 경우")
        class WhenRecordingPeriodClosed {

            @Test
            @DisplayName("기록 기간과 관계없이 참여자를 지운다")
            void shouldDeleteParticipant() {
                // given
                givenGatheringWithParticipant(CLOSED_PERIOD_OPENED_AT);

                // when
                participantCommandService.remove(command(HOST_MEMBER_ID));

                // then
                then(gatheringRepository).should().deleteParticipant(PARTICIPANT_ID);
            }
        }

        private RemoveParticipantCommand command(Long memberId) {
            return RemoveParticipantCommand.of(memberId, GATHERING_ID, PARTICIPANT_ID);
        }
    }

    @Nested
    @DisplayName("remove 메서드는(참여 토큰과 구글 계정)")
    class RemoveBySubject {

        @Nested
        @DisplayName("참여 토큰이 없는 경우")
        class WhenJoinTokenNotFound {

            @Test
            @DisplayName("예외를 던지고 모임을 읽거나 아무것도 지우지 않는다")
            void shouldThrowJoinTokenNotFoundException() {
                // given
                given(gatheringJoinTokenRepository.findGatheringIdByValue(JOIN_TOKEN))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> participantCommandService.remove(command()))
                        .isInstanceOf(JoinTokenNotFoundException.class);
                then(gatheringRepository).shouldHaveNoInteractions();
                thenNothingDeleted();
            }
        }

        @Nested
        @DisplayName("모임의 파기 시각이 지난 경우")
        class WhenGatheringExpired {

            @Test
            @DisplayName("물리 삭제 전이라도 참여 토큰을 찾지 못한 예외를 던지고 모임을 읽거나 아무것도 지우지 않는다")
            void shouldThrowJoinTokenNotFoundException() {
                // given
                givenJoinToken();
                givenRetention(GatheringRetentionFixture.EXPIRED);

                // when & then
                assertThatThrownBy(() -> participantCommandService.remove(command()))
                        .isInstanceOf(JoinTokenNotFoundException.class);
                then(gatheringRepository).shouldHaveNoInteractions();
                thenNothingDeleted();
            }
        }

        @Nested
        @DisplayName("모임이 존재하지 않는 경우")
        class WhenGatheringNotFound {

            @Test
            @DisplayName("예외를 던지고 아무것도 지우지 않는다")
            void shouldThrowGatheringNotFoundException() {
                // given
                givenJoinToken();
                givenRetention(GatheringRetentionFixture.RETAINED);
                given(gatheringRepository.findHostMemberIdByIdForUpdate(GATHERING_ID))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> participantCommandService.remove(command()))
                        .isInstanceOf(GatheringNotFoundException.class);
                thenNothingDeleted();
            }
        }

        @Nested
        @DisplayName("같은 구글 계정으로 참여한 참여자가 없는 경우")
        class WhenParticipantNotFound {

            @Test
            @DisplayName("예외를 던지고 아무것도 지우지 않는다")
            void shouldThrowParticipantNotFoundException() {
                // given
                givenJoinToken();
                givenLockedGathering();
                given(
                                participantGoogleSubjectRepository.findParticipantId(
                                        GATHERING_ID, GOOGLE_SUBJECT))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> participantCommandService.remove(command()))
                        .isInstanceOf(ParticipantNotFoundException.class);
                thenNothingDeleted();
            }
        }

        @Nested
        @DisplayName("취소하지 않은 정산 건의 결제자이거나 인원인 참여자인 경우")
        class WhenParticipantInUncancelledSettlement {

            @Test
            @DisplayName("예외를 던지고 아무것도 지우거나 익명화하지 않는다")
            void shouldThrowParticipantUnremovableException() {
                // given
                givenJoinToken();
                givenLockedGathering();
                givenGoogleParticipant();
                given(settlementRepository.existsUncancelledByParticipantId(PARTICIPANT_ID))
                        .willReturn(true);

                // when & then
                assertThatThrownBy(() -> participantCommandService.remove(command()))
                        .isInstanceOf(ParticipantUnremovableException.class);
                thenNothingDeleted();
            }
        }

        @Nested
        @DisplayName("정산 건에 전혀 없는 참여자인 경우")
        class WhenParticipantNotInAnySettlement {

            @Test
            @DisplayName("모임장의 회원 id만 읽는 잠금 조회로 모임을 잠그고 개인 링크 토큰, 피드백 제출 표시, 참여자 순서로 지운다")
            void shouldDeleteParticipant() {
                // given
                givenJoinToken();
                givenLockedGathering();
                givenGoogleParticipant();

                // when
                participantCommandService.remove(command());

                // then
                InOrder inOrder =
                        inOrder(
                                participantLinkTokenRepository,
                                participantPairingFeedbackRepository,
                                gatheringRepository);
                then(participantLinkTokenRepository)
                        .should(inOrder)
                        .deleteAllByParticipantId(PARTICIPANT_ID);
                then(participantPairingFeedbackRepository)
                        .should(inOrder)
                        .deleteByParticipantId(PARTICIPANT_ID);
                then(gatheringRepository).should(inOrder).deleteParticipant(PARTICIPANT_ID);
                then(gatheringRepository)
                        .should(never())
                        .anonymizeParticipant(anyLong(), any(LocalDateTime.class));
                then(gatheringRepository).should(never()).findByIdForUpdate(anyLong());
            }
        }

        @Nested
        @DisplayName("취소한 정산 건에만 든 참여자인 경우")
        class WhenParticipantOnlyInCancelledSettlement {

            @Test
            @DisplayName("개인 링크 토큰과 피드백 제출 표시를 지운 뒤 참여자를 익명화하고 행은 지우지 않는다")
            void shouldAnonymizeParticipant() {
                // given
                givenJoinToken();
                givenLockedGathering();
                givenGoogleParticipant();
                given(settlementRepository.existsByParticipantId(PARTICIPANT_ID)).willReturn(true);

                // when
                participantCommandService.remove(command());

                // then
                InOrder inOrder =
                        inOrder(
                                participantLinkTokenRepository,
                                participantPairingFeedbackRepository,
                                gatheringRepository);
                then(participantLinkTokenRepository)
                        .should(inOrder)
                        .deleteAllByParticipantId(PARTICIPANT_ID);
                then(participantPairingFeedbackRepository)
                        .should(inOrder)
                        .deleteByParticipantId(PARTICIPANT_ID);
                then(gatheringRepository).should(inOrder).anonymizeParticipant(PARTICIPANT_ID, NOW);
                then(gatheringRepository).should(never()).deleteParticipant(anyLong());
            }
        }

        private RemoveParticipantBySubjectCommand command() {
            return new RemoveParticipantBySubjectCommand(JOIN_TOKEN, GOOGLE_SUBJECT);
        }

        // 참여자를 읽을 필요가 없으므로 모임장의 회원 id만 읽는 잠금 조회로 모임 행만 잠근다.
        private void givenLockedGathering() {
            givenRetention(GatheringRetentionFixture.RETAINED);
            given(gatheringRepository.findHostMemberIdByIdForUpdate(GATHERING_ID))
                    .willReturn(Optional.of(HOST_MEMBER_ID));
        }

        private void givenJoinToken() {
            given(gatheringJoinTokenRepository.findGatheringIdByValue(JOIN_TOKEN))
                    .willReturn(Optional.of(GATHERING_ID));
        }

        private void givenGoogleParticipant() {
            given(
                            participantGoogleSubjectRepository.findParticipantId(
                                    GATHERING_ID, GOOGLE_SUBJECT))
                    .willReturn(Optional.of(PARTICIPANT_ID));
        }
    }

    private void thenNothingDeleted() {
        then(participantLinkTokenRepository).shouldHaveNoInteractions();
        then(participantPairingFeedbackRepository).shouldHaveNoInteractions();
        then(gatheringRepository).should(never()).deleteParticipant(anyLong());
        then(gatheringRepository)
                .should(never())
                .anonymizeParticipant(anyLong(), any(LocalDateTime.class));
    }

    // 재발급, 모임장의 제거, 참여자의 철회는 같은 모임의 다른 요청과 겹치지 않도록 모임을 잠그고 읽는다.
    private void givenGatheringWithParticipant(LocalDateTime openedAt) {
        givenRetention(GatheringRetentionFixture.RETAINED);
        givenLockedGathering(openedAt);
    }

    private void givenLockedGathering(LocalDateTime openedAt) {
        given(gatheringRepository.findByIdForUpdate(GATHERING_ID))
                .willReturn(Optional.of(gatheringWithParticipant(openedAt)));
    }

    private Gathering gatheringWithParticipant(LocalDateTime openedAt) {
        return GatheringFixture.FRIDAY_DINNER.createWithIdAndOpenedAt(
                GATHERING_ID,
                openedAt,
                List.of(ParticipantFixture.KIM.createWithId(PARTICIPANT_ID)));
    }

    private void givenRetention(GatheringRetentionFixture retention) {
        given(gatheringRetentionRepository.findByGatheringId(GATHERING_ID))
                .willReturn(Optional.of(retention.create(GATHERING_ID, NOW)));
    }
}
