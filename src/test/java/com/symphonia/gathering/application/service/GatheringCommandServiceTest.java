package com.symphonia.gathering.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.symphonia.UnitTest;
import com.symphonia.gathering.application.dto.command.JoinGatheringCommand;
import com.symphonia.gathering.application.dto.command.OpenGatheringCommand;
import com.symphonia.gathering.application.dto.command.ReissueJoinTokenCommand;
import com.symphonia.gathering.application.dto.result.GatheringJoinTokenResult;
import com.symphonia.gathering.application.dto.result.ParticipantResult;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.entity.Participant;
import com.symphonia.gathering.domain.exception.ConsentRequiredException;
import com.symphonia.gathering.domain.exception.GatheringJoinClosedException;
import com.symphonia.gathering.domain.exception.GatheringNotFoundException;
import com.symphonia.gathering.domain.exception.InvalidPhoneNumberException;
import com.symphonia.gathering.domain.exception.NotGatheringHostException;
import com.symphonia.gathering.domain.exception.OutdatedConsentVersionException;
import com.symphonia.gathering.domain.exception.ParticipantAlreadyJoinedException;
import com.symphonia.gathering.domain.policy.GatheringPolicy;
import com.symphonia.gathering.domain.provider.GatheringTokenProvider;
import com.symphonia.gathering.domain.repository.GatheringJoinTokenRepository;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.domain.repository.ParticipantLinkTokenRepository;
import com.symphonia.gathering.domain.vo.PhoneNumber;
import com.symphonia.gathering.fixture.GatheringFixture;
import com.symphonia.gathering.fixture.ParticipantFixture;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("GatheringCommandService 단위 테스트")
class GatheringCommandServiceTest extends UnitTest {

    private static final Long GATHERING_ID = 10L;
    private static final Long PARTICIPANT_ID = 20L;
    private static final Long OTHER_MEMBER_ID = 99L;
    private static final String JOIN_TOKEN = "join-token";
    private static final String NEW_JOIN_TOKEN = "new-join-token";
    private static final String LINK_TOKEN = "link-token";
    private static final String HYPHENATED_PHONE_NUMBER = "010-1234-5678";
    private static final String MASKED_PHONE_NUMBER = "010-****-5678";

    @InjectMocks private GatheringCommandService gatheringCommandService;

    @Mock private GatheringRepository gatheringRepository;
    @Mock private GatheringJoinTokenRepository gatheringJoinTokenRepository;
    @Mock private ParticipantLinkTokenRepository participantLinkTokenRepository;
    @Mock private GatheringTokenProvider gatheringTokenProvider;

    @Nested
    @DisplayName("open 메서드는")
    class Open {

        @Test
        @DisplayName("저장한 모임 id로 참여 토큰을 저장하고 반환한다")
        void shouldSaveJoinTokenWithSavedGatheringId() {
            // given
            GatheringFixture fixture = GatheringFixture.FRIDAY_DINNER;
            given(gatheringRepository.save(any(Gathering.class)))
                    .willReturn(
                            fixture.createWithIdAndCreatedAt(GATHERING_ID, LocalDateTime.now()));
            given(gatheringTokenProvider.generate()).willReturn(JOIN_TOKEN);
            OpenGatheringCommand command =
                    new OpenGatheringCommand(fixture.getHostMemberId(), fixture.getName());

            // when
            GatheringJoinTokenResult result = gatheringCommandService.open(command);

            // then
            ArgumentCaptor<Gathering> captor = ArgumentCaptor.forClass(Gathering.class);
            then(gatheringRepository).should().save(captor.capture());
            assertThat(captor.getValue().getHostMemberId()).isEqualTo(fixture.getHostMemberId());
            assertThat(captor.getValue().getName()).isEqualTo(fixture.getName());
            then(gatheringJoinTokenRepository).should().save(JOIN_TOKEN, GATHERING_ID);
            assertThat(result.gatheringId()).isEqualTo(GATHERING_ID);
            assertThat(result.joinToken()).isEqualTo(JOIN_TOKEN);
        }
    }

    @Nested
    @DisplayName("reissue 메서드는")
    class Reissue {

        @Nested
        @DisplayName("모임이 존재하지 않는 경우")
        class WhenGatheringNotFound {

            @Test
            @DisplayName("예외를 던지고 참여 토큰을 저장하지 않는다")
            void shouldThrowGatheringNotFoundException() {
                // given
                given(gatheringRepository.findById(GATHERING_ID)).willReturn(Optional.empty());
                ReissueJoinTokenCommand command =
                        ReissueJoinTokenCommand.of(
                                GatheringFixture.FRIDAY_DINNER.getHostMemberId(), GATHERING_ID);

                // when & then
                assertThatThrownBy(() -> gatheringCommandService.reissue(command))
                        .isInstanceOf(GatheringNotFoundException.class);
                then(gatheringJoinTokenRepository).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("모임을 만든 회원이 아닌 경우")
        class WhenMemberIsNotHost {

            @Test
            @DisplayName("예외를 던지고 참여 토큰을 저장하지 않는다")
            void shouldThrowNotGatheringHostException() {
                // given
                givenGathering(LocalDateTime.now().minusHours(1));
                ReissueJoinTokenCommand command =
                        ReissueJoinTokenCommand.of(OTHER_MEMBER_ID, GATHERING_ID);

                // when & then
                assertThatThrownBy(() -> gatheringCommandService.reissue(command))
                        .isInstanceOf(NotGatheringHostException.class);
                then(gatheringTokenProvider).shouldHaveNoInteractions();
                then(gatheringJoinTokenRepository).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("모임을 만든 지 48시간이 지난 경우")
        class WhenRecordingPeriodPassed {

            @Test
            @DisplayName("예외를 던지고 참여 토큰을 발급하거나 저장하지 않는다")
            void shouldThrowGatheringJoinClosedException() {
                // given
                givenGathering(LocalDateTime.now().minusHours(49));
                ReissueJoinTokenCommand command =
                        ReissueJoinTokenCommand.of(
                                GatheringFixture.FRIDAY_DINNER.getHostMemberId(), GATHERING_ID);

                // when & then
                assertThatThrownBy(() -> gatheringCommandService.reissue(command))
                        .isInstanceOf(GatheringJoinClosedException.class);
                then(gatheringTokenProvider).shouldHaveNoInteractions();
                then(gatheringJoinTokenRepository).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("모임을 만든 회원인 경우")
        class WhenMemberIsHost {

            @Test
            @DisplayName("새 참여 토큰을 저장하고 반환한다")
            void shouldSaveNewJoinToken() {
                // given
                givenGathering(LocalDateTime.now().minusHours(1));
                given(gatheringTokenProvider.generate()).willReturn(NEW_JOIN_TOKEN);
                ReissueJoinTokenCommand command =
                        ReissueJoinTokenCommand.of(
                                GatheringFixture.FRIDAY_DINNER.getHostMemberId(), GATHERING_ID);

                // when
                GatheringJoinTokenResult result = gatheringCommandService.reissue(command);

                // then
                then(gatheringJoinTokenRepository).should().save(NEW_JOIN_TOKEN, GATHERING_ID);
                assertThat(result.gatheringId()).isEqualTo(GATHERING_ID);
                assertThat(result.joinToken()).isEqualTo(NEW_JOIN_TOKEN);
            }
        }

        private void givenGathering(LocalDateTime createdAt) {
            given(gatheringRepository.findById(GATHERING_ID))
                    .willReturn(
                            Optional.of(
                                    GatheringFixture.FRIDAY_DINNER.createWithIdAndCreatedAt(
                                            GATHERING_ID, createdAt)));
        }
    }

    @Nested
    @DisplayName("join 메서드는")
    class Join {

        @Nested
        @DisplayName("참여 토큰에 해당하는 모임이 없는 경우")
        class WhenJoinTokenNotFound {

            @Test
            @DisplayName("예외를 던지고 참여자와 개인 링크 토큰을 저장하지 않는다")
            void shouldThrowGatheringNotFoundException() {
                // given
                given(gatheringJoinTokenRepository.findGatheringIdByValue(JOIN_TOKEN))
                        .willReturn(Optional.empty());
                JoinGatheringCommand command = command(GatheringPolicy.CONSENT_VERSION);

                // when & then
                assertThatThrownBy(() -> gatheringCommandService.join(command))
                        .isInstanceOf(GatheringNotFoundException.class);
                thenNothingSaved();
            }
        }

        @Nested
        @DisplayName("모임을 만든 지 48시간이 지난 경우")
        class WhenRecordingPeriodPassed {

            @Test
            @DisplayName("예외를 던지고 참여자와 개인 링크 토큰을 저장하지 않는다")
            void shouldThrowGatheringJoinClosedException() {
                // given
                givenGathering(
                        GatheringFixture.FRIDAY_DINNER.createWithIdAndCreatedAt(
                                GATHERING_ID, LocalDateTime.now().minusHours(49)));
                JoinGatheringCommand command = command(GatheringPolicy.CONSENT_VERSION);

                // when & then
                assertThatThrownBy(() -> gatheringCommandService.join(command))
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
                JoinGatheringCommand command = command(false, GatheringPolicy.CONSENT_VERSION);

                // when & then
                assertThatThrownBy(() -> gatheringCommandService.join(command))
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
                JoinGatheringCommand command = command("2000-01-01");

                // when & then
                assertThatThrownBy(() -> gatheringCommandService.join(command))
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
                JoinGatheringCommand command = command(GatheringPolicy.CONSENT_VERSION);

                // when & then
                assertThatThrownBy(() -> gatheringCommandService.join(command))
                        .isInstanceOf(ParticipantAlreadyJoinedException.class);
                thenNothingSaved();
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
                JoinGatheringCommand command =
                        new JoinGatheringCommand(
                                JOIN_TOKEN,
                                ParticipantFixture.KIM.getName(),
                                "02-123-4567",
                                true,
                                GatheringPolicy.CONSENT_VERSION);

                // when & then
                assertThatThrownBy(() -> gatheringCommandService.join(command))
                        .isInstanceOf(InvalidPhoneNumberException.class);
                thenNothingSaved();
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
                                GatheringFixture.FRIDAY_DINNER.createWithIdAndCreatedAt(
                                        GATHERING_ID,
                                        LocalDateTime.now().minusHours(1),
                                        List.of(
                                                ParticipantFixture.KIM.createWithId(
                                                        PARTICIPANT_ID))));
                given(gatheringTokenProvider.generate()).willReturn(LINK_TOKEN);
                JoinGatheringCommand command = command(GatheringPolicy.CONSENT_VERSION);

                // when
                ParticipantResult result = gatheringCommandService.join(command);

                // then
                ArgumentCaptor<Gathering> captor = ArgumentCaptor.forClass(Gathering.class);
                then(gatheringRepository).should().save(captor.capture());
                Participant joined = captor.getValue().getParticipants().getFirst();
                assertThat(joined.getPhoneNumber())
                        .isEqualTo(new PhoneNumber(ParticipantFixture.KIM.getPhoneNumber()));
                assertThat(joined.getConsentVersion()).isEqualTo(GatheringPolicy.CONSENT_VERSION);
                then(participantLinkTokenRepository).should().save(LINK_TOKEN, PARTICIPANT_ID);
                assertThat(result.id()).isEqualTo(PARTICIPANT_ID);
                assertThat(result.name()).isEqualTo(ParticipantFixture.KIM.getName());
                assertThat(result.maskedPhoneNumber()).isEqualTo(MASKED_PHONE_NUMBER);
                assertThat(result.linkToken()).isEqualTo(LINK_TOKEN);
            }
        }

        private JoinGatheringCommand command(String consentVersion) {
            return command(true, consentVersion);
        }

        private JoinGatheringCommand command(boolean consentAgreed, String consentVersion) {
            return new JoinGatheringCommand(
                    JOIN_TOKEN,
                    ParticipantFixture.KIM.getName(),
                    HYPHENATED_PHONE_NUMBER,
                    consentAgreed,
                    consentVersion);
        }

        private void givenJoinableGathering(List<Participant> participants) {
            givenGathering(
                    GatheringFixture.FRIDAY_DINNER.createWithIdAndCreatedAt(
                            GATHERING_ID, LocalDateTime.now().minusHours(1), participants));
        }

        private void givenGathering(Gathering gathering) {
            given(gatheringJoinTokenRepository.findGatheringIdByValue(JOIN_TOKEN))
                    .willReturn(Optional.of(GATHERING_ID));
            given(gatheringRepository.findById(GATHERING_ID)).willReturn(Optional.of(gathering));
        }

        private void thenNothingSaved() {
            then(gatheringRepository).should(never()).save(any(Gathering.class));
            then(participantLinkTokenRepository).should(never()).save(anyString(), anyLong());
        }
    }
}
