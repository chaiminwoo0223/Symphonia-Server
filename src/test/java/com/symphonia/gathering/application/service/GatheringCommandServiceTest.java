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
import com.symphonia.catalog.application.dto.query.CatalogReferencesQuery;
import com.symphonia.catalog.application.usecase.VerifyCatalogReferencesUseCase;
import com.symphonia.catalog.domain.exception.DrinkNotFoundException;
import com.symphonia.gathering.application.dto.command.ChooseGatheringPairingCommand;
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
import com.symphonia.gathering.domain.exception.GatheringPairingClosedException;
import com.symphonia.gathering.domain.exception.InvalidPhoneNumberException;
import com.symphonia.gathering.domain.exception.JoinTokenNotFoundException;
import com.symphonia.gathering.domain.exception.NotGatheringHostException;
import com.symphonia.gathering.domain.exception.OutdatedConsentVersionException;
import com.symphonia.gathering.domain.exception.ParticipantAlreadyJoinedException;
import com.symphonia.gathering.domain.policy.GatheringPolicy;
import com.symphonia.gathering.domain.provider.GatheringTokenProvider;
import com.symphonia.gathering.domain.repository.GatheringJoinTokenRepository;
import com.symphonia.gathering.domain.repository.GatheringPairingRepository;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.domain.repository.GatheringRetentionRepository;
import com.symphonia.gathering.domain.repository.ParticipantLinkTokenRepository;
import com.symphonia.gathering.domain.vo.PhoneNumber;
import com.symphonia.gathering.fixture.GatheringFixture;
import com.symphonia.gathering.fixture.GatheringPairingFixture;
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
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 29, 18, 0);

    // 참여 기간은 모임을 만든 지 48시간이 되는 순간 닫힌다.
    // 닫히기 1초 전과 닫히는 순간을 경계로 쓴다.
    private static final LocalDateTime OPEN_PERIOD_OPENED_AT = NOW.minusHours(48).plusSeconds(1);
    private static final LocalDateTime CLOSED_PERIOD_OPENED_AT = NOW.minusHours(48);

    private GatheringCommandService gatheringCommandService;

    @Mock private GatheringRepository gatheringRepository;
    @Mock private GatheringJoinTokenRepository gatheringJoinTokenRepository;
    @Mock private ParticipantLinkTokenRepository participantLinkTokenRepository;
    @Mock private GatheringTokenProvider gatheringTokenProvider;
    @Mock private GatheringRetentionRepository gatheringRetentionRepository;
    @Mock private GatheringPairingRepository gatheringPairingRepository;
    @Mock private VerifyCatalogReferencesUseCase verifyCatalogReferencesUseCase;

    @BeforeEach
    void setUp() {
        gatheringCommandService =
                new GatheringCommandService(
                        gatheringRepository,
                        gatheringJoinTokenRepository,
                        participantLinkTokenRepository,
                        gatheringRetentionRepository,
                        gatheringPairingRepository,
                        gatheringTokenProvider,
                        verifyCatalogReferencesUseCase,
                        Clock.fixed(
                                NOW.atZone(TimeConfig.SERVICE_ZONE).toInstant(),
                                TimeConfig.SERVICE_ZONE));
    }

    @Nested
    @DisplayName("open 메서드는")
    class Open {

        @Nested
        @DisplayName("페어링 없이 모임을 여는 경우")
        class WhenPairingAbsent {

            @Test
            @DisplayName("저장한 모임 id로 참여 토큰을 저장하고 반환한다")
            void shouldSaveJoinTokenWithSavedGatheringId() {
                // given
                GatheringFixture fixture = GatheringFixture.FRIDAY_DINNER;
                givenSavedGathering(fixture);
                OpenGatheringCommand command =
                        new OpenGatheringCommand(
                                fixture.getHostMemberId(), fixture.getName(), null);

                // when
                GatheringJoinTokenResult result = gatheringCommandService.open(command);

                // then
                ArgumentCaptor<Gathering> captor = ArgumentCaptor.forClass(Gathering.class);
                then(gatheringRepository).should().save(captor.capture());
                assertThat(captor.getValue().getHostMemberId())
                        .isEqualTo(fixture.getHostMemberId());
                assertThat(captor.getValue().getName()).isEqualTo(fixture.getName());
                then(gatheringJoinTokenRepository).should().save(JOIN_TOKEN, GATHERING_ID);
                assertThat(result.gatheringId()).isEqualTo(GATHERING_ID);
                assertThat(result.joinToken()).isEqualTo(JOIN_TOKEN);
            }

            @Test
            @DisplayName("Clock의 현재 시각을 모임을 연 시각으로 저장한다")
            void shouldSaveGatheringOpenedAtCurrentTime() {
                // given
                GatheringFixture fixture = GatheringFixture.FRIDAY_DINNER;
                givenSavedGathering(fixture);
                OpenGatheringCommand command =
                        new OpenGatheringCommand(
                                fixture.getHostMemberId(), fixture.getName(), null);

                // when
                gatheringCommandService.open(command);

                // then
                ArgumentCaptor<Gathering> captor = ArgumentCaptor.forClass(Gathering.class);
                then(gatheringRepository).should().save(captor.capture());
                assertThat(captor.getValue().getOpenedAt()).isEqualTo(NOW);
            }

            @Test
            @DisplayName("카탈로그를 검증하거나 페어링을 저장하지 않는다")
            void shouldNotSavePairing() {
                // given
                GatheringFixture fixture = GatheringFixture.FRIDAY_DINNER;
                givenSavedGathering(fixture);
                OpenGatheringCommand command =
                        new OpenGatheringCommand(
                                fixture.getHostMemberId(), fixture.getName(), null);

                // when
                gatheringCommandService.open(command);

                // then
                then(verifyCatalogReferencesUseCase).shouldHaveNoInteractions();
                then(gatheringPairingRepository).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("페어링과 함께 모임을 여는 경우")
        class WhenPairingGiven {

            @Test
            @DisplayName("카탈로그를 검증한 뒤 저장한 모임 id로 페어링을 저장하고 참여 토큰을 반환한다")
            void shouldSavePairingWithSavedGatheringId() {
                // given
                GatheringPairingFixture pairing = GatheringPairingFixture.FRIEND_CASUAL;
                givenSavedGathering(GatheringFixture.FRIDAY_DINNER);

                // when
                GatheringJoinTokenResult result = gatheringCommandService.open(command(pairing));

                // then
                then(verifyCatalogReferencesUseCase)
                        .should()
                        .verify(
                                CatalogReferencesQuery.of(
                                        pairing.getDrinkId(), pairing.getAnjuId()));
                then(gatheringPairingRepository).should().save(GATHERING_ID, pairing.create());
                assertThat(result.gatheringId()).isEqualTo(GATHERING_ID);
                assertThat(result.joinToken()).isEqualTo(JOIN_TOKEN);
            }
        }

        @Nested
        @DisplayName("페어링이 가리키는 카탈로그가 존재하지 않는 경우")
        class WhenPairingReferenceNotFound {

            @Test
            @DisplayName("카탈로그 검증 예외를 그대로 던지고 페어링과 참여 토큰을 저장하지 않는다")
            void shouldThrowDrinkNotFoundException() {
                // given
                GatheringPairingFixture pairing = GatheringPairingFixture.FRIEND_CASUAL;
                given(gatheringRepository.save(any(Gathering.class)))
                        .willReturn(
                                GatheringFixture.FRIDAY_DINNER.createWithIdAndOpenedAt(
                                        GATHERING_ID, NOW));
                willThrow(new DrinkNotFoundException())
                        .given(verifyCatalogReferencesUseCase)
                        .verify(any(CatalogReferencesQuery.class));
                OpenGatheringCommand command = command(pairing);

                // when & then
                assertThatThrownBy(() -> gatheringCommandService.open(command))
                        .isInstanceOf(DrinkNotFoundException.class);
                then(gatheringPairingRepository).shouldHaveNoInteractions();
                then(gatheringJoinTokenRepository).shouldHaveNoInteractions();
            }
        }

        private OpenGatheringCommand command(GatheringPairingFixture pairing) {
            GatheringFixture fixture = GatheringFixture.FRIDAY_DINNER;
            return new OpenGatheringCommand(
                    fixture.getHostMemberId(), fixture.getName(), pairing.createCommand());
        }

        private void givenSavedGathering(GatheringFixture fixture) {
            given(gatheringRepository.save(any(Gathering.class)))
                    .willReturn(fixture.createWithIdAndOpenedAt(GATHERING_ID, NOW));
            given(gatheringTokenProvider.generate()).willReturn(JOIN_TOKEN);
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
                given(gatheringRetentionRepository.findByGatheringId(GATHERING_ID))
                        .willReturn(Optional.empty());
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
        @DisplayName("모임의 파기 시각이 지난 경우")
        class WhenGatheringExpired {

            @Test
            @DisplayName("물리 삭제 전이라도 모임을 찾지 못한 예외를 던지고 모임을 읽거나 참여 토큰을 저장하지 않는다")
            void shouldThrowGatheringNotFoundException() {
                // given
                givenRetention(GatheringRetentionFixture.EXPIRED);
                ReissueJoinTokenCommand command =
                        ReissueJoinTokenCommand.of(
                                GatheringFixture.FRIDAY_DINNER.getHostMemberId(), GATHERING_ID);

                // when & then
                assertThatThrownBy(() -> gatheringCommandService.reissue(command))
                        .isInstanceOf(GatheringNotFoundException.class);
                then(gatheringRepository).shouldHaveNoInteractions();
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
                givenGatheringOpenedAt(OPEN_PERIOD_OPENED_AT);
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
        @DisplayName("모임을 만든 지 48시간이 된 경우")
        class WhenRecordingPeriodClosed {

            @Test
            @DisplayName("예외를 던지고 참여 토큰을 발급하거나 저장하지 않는다")
            void shouldThrowGatheringJoinClosedException() {
                // given
                givenGatheringOpenedAt(CLOSED_PERIOD_OPENED_AT);
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
                givenGatheringOpenedAt(OPEN_PERIOD_OPENED_AT);
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
    }

    @Nested
    @DisplayName("join 메서드는")
    class Join {

        @Nested
        @DisplayName("참여 토큰에 해당하는 모임이 없는 경우")
        class WhenJoinTokenNotFound {

            @Test
            @DisplayName("예외를 던지고 참여자와 개인 링크 토큰을 저장하지 않는다")
            void shouldThrowJoinTokenNotFoundException() {
                // given
                given(gatheringJoinTokenRepository.findGatheringIdByValue(JOIN_TOKEN))
                        .willReturn(Optional.empty());
                JoinGatheringCommand command = command(GatheringPolicy.CONSENT_VERSION);

                // when & then
                assertThatThrownBy(() -> gatheringCommandService.join(command))
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
                JoinGatheringCommand command = command(GatheringPolicy.CONSENT_VERSION);

                // when & then
                assertThatThrownBy(() -> gatheringCommandService.join(command))
                        .isInstanceOf(JoinTokenNotFoundException.class);
                then(gatheringRepository).shouldHaveNoInteractions();
                then(participantLinkTokenRepository).shouldHaveNoInteractions();
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
                                GatheringFixture.FRIDAY_DINNER.createWithIdAndOpenedAt(
                                        GATHERING_ID,
                                        OPEN_PERIOD_OPENED_AT,
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
                    GatheringFixture.FRIDAY_DINNER.createWithIdAndOpenedAt(
                            GATHERING_ID, OPEN_PERIOD_OPENED_AT, participants));
        }

        private void givenGathering(Gathering gathering) {
            given(gatheringJoinTokenRepository.findGatheringIdByValue(JOIN_TOKEN))
                    .willReturn(Optional.of(GATHERING_ID));
            givenRetention(GatheringRetentionFixture.RETAINED);
            given(gatheringRepository.findById(GATHERING_ID)).willReturn(Optional.of(gathering));
        }

        private void thenNothingSaved() {
            then(gatheringRepository).should(never()).save(any(Gathering.class));
            then(participantLinkTokenRepository).should(never()).save(anyString(), anyLong());
        }
    }

    @Nested
    @DisplayName("choose 메서드는")
    class Choose {

        @Nested
        @DisplayName("모임이 존재하지 않는 경우")
        class WhenGatheringNotFound {

            @Test
            @DisplayName("예외를 던지고 페어링을 저장하지 않는다")
            void shouldThrowGatheringNotFoundException() {
                // given
                given(gatheringRetentionRepository.findByGatheringId(GATHERING_ID))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> gatheringCommandService.choose(hostCommand()))
                        .isInstanceOf(GatheringNotFoundException.class);
                thenPairingNotSaved();
            }
        }

        @Nested
        @DisplayName("모임의 파기 시각이 지난 경우")
        class WhenGatheringExpired {

            @Test
            @DisplayName("물리 삭제 전이라도 모임을 찾지 못한 예외를 던지고 모임을 읽거나 페어링을 저장하지 않는다")
            void shouldThrowGatheringNotFoundException() {
                // given
                givenRetention(GatheringRetentionFixture.EXPIRED);

                // when & then
                assertThatThrownBy(() -> gatheringCommandService.choose(hostCommand()))
                        .isInstanceOf(GatheringNotFoundException.class);
                then(gatheringRepository).shouldHaveNoInteractions();
                thenPairingNotSaved();
            }
        }

        @Nested
        @DisplayName("모임을 만든 회원이 아닌 경우")
        class WhenMemberIsNotHost {

            @Test
            @DisplayName("예외를 던지고 페어링을 저장하지 않는다")
            void shouldThrowNotGatheringHostException() {
                // given
                givenGatheringOpenedAt(OPEN_PERIOD_OPENED_AT);
                ChooseGatheringPairingCommand command =
                        ChooseGatheringPairingCommand.of(
                                OTHER_MEMBER_ID,
                                GATHERING_ID,
                                GatheringPairingFixture.FRIEND_CASUAL.createCommand());

                // when & then
                assertThatThrownBy(() -> gatheringCommandService.choose(command))
                        .isInstanceOf(NotGatheringHostException.class);
                thenPairingNotSaved();
            }
        }

        @Nested
        @DisplayName("모임을 만든 지 48시간이 된 경우")
        class WhenRecordingPeriodClosed {

            @Test
            @DisplayName("예외를 던지고 카탈로그를 검증하거나 페어링을 저장하지 않는다")
            void shouldThrowGatheringPairingClosedException() {
                // given
                givenGatheringOpenedAt(CLOSED_PERIOD_OPENED_AT);

                // when & then
                assertThatThrownBy(() -> gatheringCommandService.choose(hostCommand()))
                        .isInstanceOf(GatheringPairingClosedException.class);
                thenPairingNotSaved();
            }
        }

        @Nested
        @DisplayName("페어링이 가리키는 카탈로그가 존재하지 않는 경우")
        class WhenPairingReferenceNotFound {

            @Test
            @DisplayName("카탈로그 검증 예외를 그대로 던지고 페어링을 저장하지 않는다")
            void shouldThrowDrinkNotFoundException() {
                // given
                givenGatheringOpenedAt(OPEN_PERIOD_OPENED_AT);
                willThrow(new DrinkNotFoundException())
                        .given(verifyCatalogReferencesUseCase)
                        .verify(any(CatalogReferencesQuery.class));

                // when & then
                assertThatThrownBy(() -> gatheringCommandService.choose(hostCommand()))
                        .isInstanceOf(DrinkNotFoundException.class);
                then(gatheringPairingRepository).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("모임장이 기록 기간 안에 고르는 경우")
        class WhenChangeable {

            @Test
            @DisplayName("48시간이 되기 1초 전이면 카탈로그를 검증한 뒤 모임의 페어링으로 저장한다")
            void shouldSavePairingBeforeRecordingPeriodCloses() {
                // given
                givenGatheringOpenedAt(OPEN_PERIOD_OPENED_AT);
                GatheringPairingFixture pairing = GatheringPairingFixture.LOVER_ROMANTIC;
                ChooseGatheringPairingCommand command =
                        ChooseGatheringPairingCommand.of(
                                GatheringFixture.FRIDAY_DINNER.getHostMemberId(),
                                GATHERING_ID,
                                pairing.createCommand());

                // when
                gatheringCommandService.choose(command);

                // then
                then(verifyCatalogReferencesUseCase)
                        .should()
                        .verify(
                                CatalogReferencesQuery.of(
                                        pairing.getDrinkId(), pairing.getAnjuId()));
                then(gatheringPairingRepository).should().save(GATHERING_ID, pairing.create());
            }
        }

        private ChooseGatheringPairingCommand hostCommand() {
            return ChooseGatheringPairingCommand.of(
                    GatheringFixture.FRIDAY_DINNER.getHostMemberId(),
                    GATHERING_ID,
                    GatheringPairingFixture.FRIEND_CASUAL.createCommand());
        }

        private void thenPairingNotSaved() {
            then(verifyCatalogReferencesUseCase).shouldHaveNoInteractions();
            then(gatheringPairingRepository).shouldHaveNoInteractions();
        }
    }

    private void givenGatheringOpenedAt(LocalDateTime openedAt) {
        givenRetention(GatheringRetentionFixture.RETAINED);
        given(gatheringRepository.findById(GATHERING_ID))
                .willReturn(
                        Optional.of(
                                GatheringFixture.FRIDAY_DINNER.createWithIdAndOpenedAt(
                                        GATHERING_ID, openedAt)));
    }

    private void givenRetention(GatheringRetentionFixture retention) {
        given(gatheringRetentionRepository.findByGatheringId(GATHERING_ID))
                .willReturn(Optional.of(retention.create(GATHERING_ID, NOW)));
    }
}
