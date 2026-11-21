package com.symphonia.gathering.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;

import com.symphonia.UnitTest;
import com.symphonia.catalog.application.dto.query.CatalogReferencesQuery;
import com.symphonia.catalog.application.usecase.VerifyCatalogReferencesUseCase;
import com.symphonia.catalog.domain.exception.DrinkNotFoundException;
import com.symphonia.gathering.application.dto.command.ChooseGatheringPairingCommand;
import com.symphonia.gathering.application.dto.command.DeleteGatheringCommand;
import com.symphonia.gathering.application.dto.command.ExtendRecordingCommand;
import com.symphonia.gathering.application.dto.command.OpenGatheringCommand;
import com.symphonia.gathering.application.dto.command.ReissueJoinTokenCommand;
import com.symphonia.gathering.application.dto.command.RenameGatheringCommand;
import com.symphonia.gathering.application.dto.result.GatheringJoinTokenResult;
import com.symphonia.gathering.application.dto.result.RecordingExtensionResult;
import com.symphonia.gathering.application.usecase.PurgeGatheringUseCase;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.exception.GatheringJoinClosedException;
import com.symphonia.gathering.domain.exception.GatheringNotFoundException;
import com.symphonia.gathering.domain.exception.GatheringOpenConflictException;
import com.symphonia.gathering.domain.exception.GatheringPairingClosedException;
import com.symphonia.gathering.domain.exception.GatheringRecordingExtensionClosedException;
import com.symphonia.gathering.domain.exception.NotGatheringHostException;
import com.symphonia.gathering.domain.provider.GatheringTokenProvider;
import com.symphonia.gathering.domain.repository.GatheringJoinTokenRepository;
import com.symphonia.gathering.domain.repository.GatheringPairingRepository;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.domain.repository.GatheringRetentionRepository;
import com.symphonia.gathering.fixture.GatheringFixture;
import com.symphonia.gathering.fixture.GatheringPairingFixture;
import com.symphonia.gathering.fixture.GatheringRetentionFixture;
import com.symphonia.global.config.TimeConfig;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;

@DisplayName("GatheringCommandService 단위 테스트")
class GatheringCommandServiceTest extends UnitTest {

    private static final Long GATHERING_ID = 10L;
    private static final Long OTHER_MEMBER_ID = 99L;
    private static final UUID CLIENT_REQUEST_ID =
            UUID.fromString("0b6c1a52-6f4e-4d0a-9d52-7a1f3c1e8b11");
    private static final String JOIN_TOKEN = "join-token";
    private static final String NEW_JOIN_TOKEN = "new-join-token";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 29, 18, 0);

    // 참여 기간은 모임을 만든 지 48시간이 되는 순간 닫힌다.
    // 닫히기 1초 전과 닫히는 순간을 경계로 쓴다.
    private static final LocalDateTime OPEN_PERIOD_OPENED_AT = NOW.minusHours(48).plusSeconds(1);
    private static final LocalDateTime CLOSED_PERIOD_OPENED_AT = NOW.minusHours(48);

    private GatheringCommandService gatheringCommandService;

    @Mock private GatheringRepository gatheringRepository;
    @Mock private GatheringJoinTokenRepository gatheringJoinTokenRepository;
    @Mock private GatheringTokenProvider gatheringTokenProvider;
    @Mock private GatheringRetentionRepository gatheringRetentionRepository;
    @Mock private GatheringPairingRepository gatheringPairingRepository;
    @Mock private VerifyCatalogReferencesUseCase verifyCatalogReferencesUseCase;
    @Mock private PurgeGatheringUseCase purgeGatheringUseCase;

    @BeforeEach
    void setUp() {
        gatheringCommandService =
                new GatheringCommandService(
                        gatheringRepository,
                        gatheringJoinTokenRepository,
                        gatheringRetentionRepository,
                        gatheringPairingRepository,
                        gatheringTokenProvider,
                        verifyCatalogReferencesUseCase,
                        purgeGatheringUseCase,
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
                                fixture.getHostMemberId(),
                                CLIENT_REQUEST_ID,
                                fixture.getName(),
                                null);

                // when
                GatheringJoinTokenResult result = gatheringCommandService.open(command);

                // then
                ArgumentCaptor<Gathering> captor = ArgumentCaptor.forClass(Gathering.class);
                then(gatheringRepository).should().saveIfAbsent(captor.capture());
                assertThat(captor.getValue().getHostMemberId())
                        .isEqualTo(fixture.getHostMemberId());
                assertThat(captor.getValue().getName()).isEqualTo(fixture.getName());
                then(gatheringJoinTokenRepository).should().save(JOIN_TOKEN, GATHERING_ID);
                assertThat(result.gatheringId()).isEqualTo(GATHERING_ID);
                assertThat(result.joinToken()).isEqualTo(JOIN_TOKEN);
            }

            @Test
            @DisplayName("참여 토큰의 만료 시각으로 모임을 연 지 48시간이 되는 시각을 반환한다")
            void shouldReturnExpiresAtAfterRecordingPeriod() {
                // given
                GatheringFixture fixture = GatheringFixture.FRIDAY_DINNER;
                givenSavedGathering(fixture);
                OpenGatheringCommand command =
                        new OpenGatheringCommand(
                                fixture.getHostMemberId(),
                                CLIENT_REQUEST_ID,
                                fixture.getName(),
                                null);

                // when
                GatheringJoinTokenResult result = gatheringCommandService.open(command);

                // then
                assertThat(result.expiresAt()).isEqualTo(NOW.plusHours(48));
            }

            @Test
            @DisplayName("Clock의 현재 시각을 모임을 연 시각으로 저장한다")
            void shouldSaveGatheringOpenedAtCurrentTime() {
                // given
                GatheringFixture fixture = GatheringFixture.FRIDAY_DINNER;
                givenSavedGathering(fixture);
                OpenGatheringCommand command =
                        new OpenGatheringCommand(
                                fixture.getHostMemberId(),
                                CLIENT_REQUEST_ID,
                                fixture.getName(),
                                null);

                // when
                gatheringCommandService.open(command);

                // then
                ArgumentCaptor<Gathering> captor = ArgumentCaptor.forClass(Gathering.class);
                then(gatheringRepository).should().saveIfAbsent(captor.capture());
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
                                fixture.getHostMemberId(),
                                CLIENT_REQUEST_ID,
                                fixture.getName(),
                                null);

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
                given(gatheringRepository.saveIfAbsent(any(Gathering.class)))
                        .willReturn(
                                Optional.of(
                                        GatheringFixture.FRIDAY_DINNER.createWithIdAndOpenedAt(
                                                GATHERING_ID, NOW)));
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

        @Nested
        @DisplayName("같은 요청 id로 이미 연 모임이 있는 경우")
        class WhenAlreadyOpened {

            @Test
            @DisplayName("모임을 새로 저장하지 않고 처음 연 모임에 참여 토큰을 새로 발급해 반환한다")
            void shouldReissueJoinTokenOfOpenedGathering() {
                // given
                GatheringFixture fixture = GatheringFixture.FRIDAY_DINNER;
                given(
                                gatheringRepository.findByHostMemberIdAndClientRequestId(
                                        fixture.getHostMemberId(), CLIENT_REQUEST_ID))
                        .willReturn(
                                Optional.of(fixture.createWithIdAndOpenedAt(GATHERING_ID, NOW)));
                given(gatheringTokenProvider.generate()).willReturn(NEW_JOIN_TOKEN);

                // when
                GatheringJoinTokenResult result =
                        gatheringCommandService.open(
                                command(GatheringPairingFixture.FRIEND_CASUAL));

                // then
                then(gatheringRepository).should(never()).saveIfAbsent(any(Gathering.class));
                then(gatheringJoinTokenRepository).should().save(NEW_JOIN_TOKEN, GATHERING_ID);
                assertThat(result.gatheringId()).isEqualTo(GATHERING_ID);
                assertThat(result.joinToken()).isEqualTo(NEW_JOIN_TOKEN);
            }

            @Test
            @DisplayName("요청한 페어링을 검증하거나 저장하지 않는다")
            void shouldNotSavePairing() {
                // given
                GatheringFixture fixture = GatheringFixture.FRIDAY_DINNER;
                given(
                                gatheringRepository.findByHostMemberIdAndClientRequestId(
                                        fixture.getHostMemberId(), CLIENT_REQUEST_ID))
                        .willReturn(
                                Optional.of(fixture.createWithIdAndOpenedAt(GATHERING_ID, NOW)));
                given(gatheringTokenProvider.generate()).willReturn(NEW_JOIN_TOKEN);

                // when
                gatheringCommandService.open(command(GatheringPairingFixture.FRIEND_CASUAL));

                // then
                then(verifyCatalogReferencesUseCase).shouldHaveNoInteractions();
                then(gatheringPairingRepository).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("같은 요청 id의 모임을 동시에 넣어 먼저 넣은 요청에 진 경우")
        class WhenLostRace {

            @Test
            @DisplayName("예외를 던지고 참여 토큰을 발급하거나 페어링을 저장하지 않는다")
            void shouldThrowGatheringOpenConflictException() {
                // given
                given(gatheringRepository.saveIfAbsent(any(Gathering.class)))
                        .willReturn(Optional.empty());
                OpenGatheringCommand command = command(GatheringPairingFixture.FRIEND_CASUAL);

                // when & then
                assertThatThrownBy(() -> gatheringCommandService.open(command))
                        .isInstanceOf(GatheringOpenConflictException.class);
                then(gatheringJoinTokenRepository).shouldHaveNoInteractions();
                then(gatheringPairingRepository).shouldHaveNoInteractions();
            }
        }

        private OpenGatheringCommand command(GatheringPairingFixture pairing) {
            GatheringFixture fixture = GatheringFixture.FRIDAY_DINNER;
            return new OpenGatheringCommand(
                    fixture.getHostMemberId(),
                    CLIENT_REQUEST_ID,
                    fixture.getName(),
                    pairing.createCommand());
        }

        private void givenSavedGathering(GatheringFixture fixture) {
            given(gatheringRepository.saveIfAbsent(any(Gathering.class)))
                    .willReturn(Optional.of(fixture.createWithIdAndOpenedAt(GATHERING_ID, NOW)));
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
        @DisplayName("기록 기간을 연장한 모임인 경우")
        class WhenRecordingExtended {

            @Test
            @DisplayName("모임을 만든 지 48시간이 지나도 재발급하고 만료 시각으로 96시간이 되는 시각을 반환한다")
            void shouldReissueAndReturnExtendedExpiresAt() {
                // given
                givenExtendedGatheringOpenedAt(CLOSED_PERIOD_OPENED_AT);
                given(gatheringTokenProvider.generate()).willReturn(NEW_JOIN_TOKEN);
                ReissueJoinTokenCommand command =
                        ReissueJoinTokenCommand.of(
                                GatheringFixture.FRIDAY_DINNER.getHostMemberId(), GATHERING_ID);

                // when
                GatheringJoinTokenResult result = gatheringCommandService.reissue(command);

                // then
                then(gatheringJoinTokenRepository).should().save(NEW_JOIN_TOKEN, GATHERING_ID);
                assertThat(result.expiresAt()).isEqualTo(CLOSED_PERIOD_OPENED_AT.plusHours(96));
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

            @Test
            @DisplayName("재발급한 참여 토큰의 만료 시각으로 모임을 연 지 48시간이 되는 시각을 반환한다")
            void shouldReturnExpiresAtAfterRecordingPeriod() {
                // given
                givenGatheringOpenedAt(OPEN_PERIOD_OPENED_AT);
                given(gatheringTokenProvider.generate()).willReturn(NEW_JOIN_TOKEN);
                ReissueJoinTokenCommand command =
                        ReissueJoinTokenCommand.of(
                                GatheringFixture.FRIDAY_DINNER.getHostMemberId(), GATHERING_ID);

                // when
                GatheringJoinTokenResult result = gatheringCommandService.reissue(command);

                // then
                assertThat(result.expiresAt()).isEqualTo(OPEN_PERIOD_OPENED_AT.plusHours(48));
            }
        }
    }

    @Nested
    @DisplayName("rename 메서드는")
    class Rename {

        private static final String NEW_NAME = "토요일 번개";

        @Nested
        @DisplayName("모임이 존재하지 않는 경우")
        class WhenGatheringNotFound {

            @Test
            @DisplayName("예외를 던지고 이름을 바꾸지 않는다")
            void shouldThrowGatheringNotFoundException() {
                // given
                given(gatheringRetentionRepository.findByGatheringId(GATHERING_ID))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> gatheringCommandService.rename(hostCommand()))
                        .isInstanceOf(GatheringNotFoundException.class);
                then(gatheringRepository).shouldHaveNoInteractions();
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
                assertThatThrownBy(() -> gatheringCommandService.rename(hostCommand()))
                        .isInstanceOf(GatheringNotFoundException.class);
                then(gatheringRepository).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("모임을 만든 회원이 아닌 경우")
        class WhenMemberIsNotHost {

            @Test
            @DisplayName("예외를 던지고 이름을 바꾸지 않는다")
            void shouldThrowNotGatheringHostException() {
                // given
                givenGatheringOpenedAt(OPEN_PERIOD_OPENED_AT);
                RenameGatheringCommand command =
                        RenameGatheringCommand.of(OTHER_MEMBER_ID, GATHERING_ID, NEW_NAME);

                // when & then
                assertThatThrownBy(() -> gatheringCommandService.rename(command))
                        .isInstanceOf(NotGatheringHostException.class);
                then(gatheringRepository).should(never()).updateName(anyLong(), anyString());
            }
        }

        @Nested
        @DisplayName("기록 기간이 끝난 모임인 경우")
        class WhenRecordingPeriodClosed {

            @Test
            @DisplayName("기록 기간과 관계없이 이름을 바꾼다")
            void shouldUpdateName() {
                // given
                givenGatheringOpenedAt(CLOSED_PERIOD_OPENED_AT);

                // when
                gatheringCommandService.rename(hostCommand());

                // then
                then(gatheringRepository).should().updateName(GATHERING_ID, NEW_NAME);
            }
        }

        @Nested
        @DisplayName("모임을 만든 회원이 요청하는 경우")
        class WhenMemberIsHost {

            @Test
            @DisplayName("모임 이름을 요청한 이름으로 바꾼다")
            void shouldUpdateName() {
                // given
                givenGatheringOpenedAt(OPEN_PERIOD_OPENED_AT);

                // when
                gatheringCommandService.rename(hostCommand());

                // then
                then(gatheringRepository).should().updateName(GATHERING_ID, NEW_NAME);
            }
        }

        private RenameGatheringCommand hostCommand() {
            return RenameGatheringCommand.of(
                    GatheringFixture.FRIDAY_DINNER.getHostMemberId(), GATHERING_ID, NEW_NAME);
        }
    }

    @Nested
    @DisplayName("delete 메서드는")
    class Delete {

        private static final Long HOST_MEMBER_ID = 1L;

        @Test
        @DisplayName("데이터 키를 지운 뒤에 모임의 나머지 행을 선택 신호 없이 지운다")
        void shouldPurgeAfterDiscardingDataKey() {
            // when
            gatheringCommandService.delete(DeleteGatheringCommand.of(HOST_MEMBER_ID, GATHERING_ID));

            // then
            InOrder inOrder = inOrder(purgeGatheringUseCase);
            inOrder.verify(purgeGatheringUseCase)
                    .discardDataKeyOfHostedGathering(HOST_MEMBER_ID, GATHERING_ID);
            inOrder.verify(purgeGatheringUseCase).purgeWithoutSelectionSignal(GATHERING_ID);
        }
    }

    @Nested
    @DisplayName("extend 메서드는")
    class Extend {

        @Nested
        @DisplayName("모임이 존재하지 않는 경우")
        class WhenGatheringNotFound {

            @Test
            @DisplayName("예외를 던지고 연장하지 않는다")
            void shouldThrowGatheringNotFoundException() {
                // given
                given(gatheringRetentionRepository.findByGatheringId(GATHERING_ID))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> gatheringCommandService.extend(hostCommand()))
                        .isInstanceOf(GatheringNotFoundException.class);
                then(gatheringRepository).shouldHaveNoInteractions();
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
                assertThatThrownBy(() -> gatheringCommandService.extend(hostCommand()))
                        .isInstanceOf(GatheringNotFoundException.class);
                then(gatheringRepository).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("모임을 만든 회원이 아닌 경우")
        class WhenMemberIsNotHost {

            @Test
            @DisplayName("예외를 던지고 연장하지 않는다")
            void shouldThrowNotGatheringHostException() {
                // given
                givenGatheringOpenedAt(OPEN_PERIOD_OPENED_AT);
                ExtendRecordingCommand command =
                        ExtendRecordingCommand.of(OTHER_MEMBER_ID, GATHERING_ID);

                // when & then
                assertThatThrownBy(() -> gatheringCommandService.extend(command))
                        .isInstanceOf(NotGatheringHostException.class);
                then(gatheringRepository).should(never()).markRecordingExtended(anyLong());
            }
        }

        @Nested
        @DisplayName("기록 기간이 끝난 경우")
        class WhenRecordingPeriodClosed {

            @Test
            @DisplayName("모임을 만든 지 48시간이 된 모임은 예외를 던지고 연장하지 않는다")
            void shouldThrowGatheringRecordingExtensionClosedException() {
                // given
                givenGatheringOpenedAt(CLOSED_PERIOD_OPENED_AT);

                // when & then
                assertThatThrownBy(() -> gatheringCommandService.extend(hostCommand()))
                        .isInstanceOf(GatheringRecordingExtensionClosedException.class);
                then(gatheringRepository).should(never()).markRecordingExtended(anyLong());
            }
        }

        @Nested
        @DisplayName("기록 기간이 끝나기 전에 처음 연장하는 경우")
        class WhenExtendable {

            @Test
            @DisplayName("48시간이 되기 1초 전이면 연장했다고 표시하고 48시간 더한 시각을 반환한다")
            void shouldMarkRecordingExtendedAndReturnExtendedClosesAt() {
                // given
                givenGatheringOpenedAt(OPEN_PERIOD_OPENED_AT);

                // when
                RecordingExtensionResult result = gatheringCommandService.extend(hostCommand());

                // then
                then(gatheringRepository).should().markRecordingExtended(GATHERING_ID);
                assertThat(result.recordingClosesAt())
                        .isEqualTo(OPEN_PERIOD_OPENED_AT.plusHours(96));
            }
        }

        @Nested
        @DisplayName("이미 연장한 모임인 경우")
        class WhenAlreadyExtended {

            @Test
            @DisplayName("다시 연장하지 않고 연장한 기록 기간이 끝나는 시각을 반환한다")
            void shouldReturnExtendedClosesAtWithoutMarkingAgain() {
                // given
                givenExtendedGatheringOpenedAt(CLOSED_PERIOD_OPENED_AT);

                // when
                RecordingExtensionResult result = gatheringCommandService.extend(hostCommand());

                // then
                then(gatheringRepository).should(never()).markRecordingExtended(anyLong());
                assertThat(result.recordingClosesAt())
                        .isEqualTo(CLOSED_PERIOD_OPENED_AT.plusHours(96));
            }

            @Test
            @DisplayName("연장한 기록 기간도 끝났으면 예외 없이 끝난 시각을 반환한다")
            void shouldReturnClosedExtendedClosesAtWithoutThrowing() {
                // given
                givenExtendedGatheringOpenedAt(NOW.minusHours(96));

                // when
                RecordingExtensionResult result = gatheringCommandService.extend(hostCommand());

                // then
                then(gatheringRepository).should(never()).markRecordingExtended(anyLong());
                assertThat(result.recordingClosesAt()).isEqualTo(NOW);
            }
        }

        private ExtendRecordingCommand hostCommand() {
            return ExtendRecordingCommand.of(
                    GatheringFixture.FRIDAY_DINNER.getHostMemberId(), GATHERING_ID);
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

    private void givenExtendedGatheringOpenedAt(LocalDateTime openedAt) {
        givenRetention(GatheringRetentionFixture.RETAINED);
        given(gatheringRepository.findById(GATHERING_ID))
                .willReturn(
                        Optional.of(
                                GatheringFixture.FRIDAY_DINNER.createExtendedWithIdAndOpenedAt(
                                        GATHERING_ID, openedAt)));
    }

    private void givenRetention(GatheringRetentionFixture retention) {
        given(gatheringRetentionRepository.findByGatheringId(GATHERING_ID))
                .willReturn(Optional.of(retention.create(GATHERING_ID, NOW)));
    }
}
