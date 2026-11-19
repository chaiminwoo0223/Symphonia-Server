package com.symphonia.gathering.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.symphonia.UnitTest;
import com.symphonia.gathering.application.dto.command.AddRoundCommand;
import com.symphonia.gathering.application.dto.result.GatheringRoundResult;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.entity.GatheringRound;
import com.symphonia.gathering.domain.exception.GatheringNotFoundException;
import com.symphonia.gathering.domain.exception.GatheringRoundClosedException;
import com.symphonia.gathering.domain.exception.GatheringRoundCountExceedsLimitException;
import com.symphonia.gathering.domain.exception.InvalidGatheringRoundNumberException;
import com.symphonia.gathering.domain.exception.NotGatheringHostException;
import com.symphonia.gathering.domain.policy.GatheringPolicy;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.domain.repository.GatheringRetentionRepository;
import com.symphonia.gathering.fixture.GatheringFixture;
import com.symphonia.gathering.fixture.GatheringRetentionFixture;
import com.symphonia.gathering.fixture.GatheringRoundFixture;
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

@DisplayName("RoundCommandService 단위 테스트")
class RoundCommandServiceTest extends UnitTest {

    private static final Long GATHERING_ID = 10L;
    private static final Long SAVED_ROUND_ID = 900L;
    private static final Long OTHER_MEMBER_ID = 99L;
    private static final Long HOST_MEMBER_ID = GatheringFixture.FRIDAY_DINNER.getHostMemberId();
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 29, 18, 0);

    // 차수는 모임을 만든 지 48시간이 되는 순간부터 추가할 수 없다.
    // 닫히기 1초 전과 닫히는 순간을 경계로 쓴다.
    private static final LocalDateTime OPEN_PERIOD_OPENED_AT = NOW.minusHours(48).plusSeconds(1);
    private static final LocalDateTime CLOSED_PERIOD_OPENED_AT = NOW.minusHours(48);

    private RoundCommandService roundCommandService;

    @Mock private GatheringRepository gatheringRepository;
    @Mock private GatheringRetentionRepository gatheringRetentionRepository;

    @BeforeEach
    void setUp() {
        Clock clock =
                Clock.fixed(
                        NOW.atZone(TimeConfig.SERVICE_ZONE).toInstant(), TimeConfig.SERVICE_ZONE);
        roundCommandService =
                new RoundCommandService(
                        gatheringRepository, gatheringRetentionRepository, clock);
    }

    @Nested
    @DisplayName("add 메서드는")
    class Add {

        @Nested
        @DisplayName("모임의 파기 시각이 없는 경우")
        class WhenRetentionNotFound {

            @Test
            @DisplayName("예외를 던지고 모임을 읽지 않는다")
            void shouldThrowGatheringNotFoundException() {
                // given
                given(gatheringRetentionRepository.findByGatheringId(GATHERING_ID))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> roundCommandService.add(command(HOST_MEMBER_ID, 2)))
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
                assertThatThrownBy(() -> roundCommandService.add(command(HOST_MEMBER_ID, 2)))
                        .isInstanceOf(GatheringNotFoundException.class);
                then(gatheringRepository).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("모임이 없는 경우")
        class WhenGatheringNotFound {

            @Test
            @DisplayName("예외를 던지고 모임을 저장하지 않는다")
            void shouldThrowGatheringNotFoundException() {
                // given
                givenRetention(GatheringRetentionFixture.RETAINED);
                given(gatheringRepository.findById(GATHERING_ID)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> roundCommandService.add(command(HOST_MEMBER_ID, 2)))
                        .isInstanceOf(GatheringNotFoundException.class);
                thenGatheringNotSaved();
            }
        }

        @Nested
        @DisplayName("모임을 만든 회원이 아닌 경우")
        class WhenMemberIsNotHost {

            @Test
            @DisplayName("예외를 던지고 모임을 저장하지 않는다")
            void shouldThrowNotGatheringHostException() {
                // given
                givenGathering(OPEN_PERIOD_OPENED_AT, List.of(first()));

                // when & then
                assertThatThrownBy(() -> roundCommandService.add(command(OTHER_MEMBER_ID, 2)))
                        .isInstanceOf(NotGatheringHostException.class);
                thenGatheringNotSaved();
            }

            @Test
            @DisplayName("이미 있는 번호를 요청해도 예외를 던진다")
            void shouldThrowNotGatheringHostExceptionWhenRoundExists() {
                // given
                givenGathering(OPEN_PERIOD_OPENED_AT, List.of(first()));

                // when & then
                assertThatThrownBy(() -> roundCommandService.add(command(OTHER_MEMBER_ID, 1)))
                        .isInstanceOf(NotGatheringHostException.class);
                thenGatheringNotSaved();
            }
        }

        @Nested
        @DisplayName("모임을 만든 지 48시간이 된 경우")
        class WhenRecordingPeriodClosed {

            @Test
            @DisplayName("예외를 던지고 모임을 저장하지 않는다")
            void shouldThrowGatheringRoundClosedException() {
                // given
                givenGathering(CLOSED_PERIOD_OPENED_AT, List.of(first()));

                // when & then
                assertThatThrownBy(() -> roundCommandService.add(command(HOST_MEMBER_ID, 2)))
                        .isInstanceOf(GatheringRoundClosedException.class);
                thenGatheringNotSaved();
            }

            @Test
            @DisplayName("이미 있는 번호를 요청해도 예외를 던진다")
            void shouldThrowGatheringRoundClosedExceptionWhenRoundExists() {
                // given
                givenGathering(CLOSED_PERIOD_OPENED_AT, List.of(first()));

                // when & then
                assertThatThrownBy(() -> roundCommandService.add(command(HOST_MEMBER_ID, 1)))
                        .isInstanceOf(GatheringRoundClosedException.class);
                thenGatheringNotSaved();
            }
        }

        @Nested
        @DisplayName("기록 기간이 끝나기 1초 전인 경우")
        class WhenRecordingPeriodAboutToClose {

            @Test
            @DisplayName("다음 차수를 추가한다")
            void shouldAddNextRound() {
                // given
                givenGathering(OPEN_PERIOD_OPENED_AT, List.of(first()));
                givenSaveAssignsRoundId();

                // when
                GatheringRoundResult result = roundCommandService.add(command(HOST_MEMBER_ID, 2));

                // then
                assertThat(result.number()).isEqualTo(2);
            }
        }

        @Nested
        @DisplayName("마지막 차수의 다음 번호를 요청한 경우")
        class WhenNextRoundNumber {

            @Test
            @DisplayName("그 번호의 차수를 더해 저장하고, 저장된 차수의 id와 번호를 반환한다")
            void shouldSaveGatheringWithNewRound() {
                // given
                givenGathering(OPEN_PERIOD_OPENED_AT, List.of(first()));
                givenSaveAssignsRoundId();

                // when
                GatheringRoundResult result = roundCommandService.add(command(HOST_MEMBER_ID, 2));

                // then
                ArgumentCaptor<Gathering> captor = ArgumentCaptor.forClass(Gathering.class);
                then(gatheringRepository).should().save(captor.capture());
                assertThat(captor.getValue().getRounds())
                        .extracting(GatheringRound::getNumber)
                        .containsExactly(1, 2);
                assertThat(result.id()).isEqualTo(SAVED_ROUND_ID);
                assertThat(result.number()).isEqualTo(2);
            }

            @Test
            @DisplayName("상한인 10차도 추가한다")
            void shouldAddRoundAtLimit() {
                // given
                int limit = GatheringPolicy.MAX_ROUND_COUNT;
                givenGathering(
                        OPEN_PERIOD_OPENED_AT, GatheringRoundFixture.createUpTo(limit - 1));
                givenSaveAssignsRoundId();

                // when
                GatheringRoundResult result = roundCommandService.add(command(HOST_MEMBER_ID, limit));

                // then
                assertThat(result.number()).isEqualTo(limit);
            }
        }

        @Nested
        @DisplayName("이미 있는 번호를 요청한 경우")
        class WhenRoundAlreadyExists {

            @Test
            @DisplayName("새로 만들지 않고 저장하지 않으며 그 차수를 반환한다")
            void shouldReturnExistingRound() {
                // given
                givenGathering(
                        OPEN_PERIOD_OPENED_AT,
                        List.of(first(), GatheringRoundFixture.SECOND.create()));

                // when
                GatheringRoundResult result = roundCommandService.add(command(HOST_MEMBER_ID, 2));

                // then
                thenGatheringNotSaved();
                assertThat(result.id()).isEqualTo(GatheringRoundFixture.SECOND.getId());
                assertThat(result.number()).isEqualTo(GatheringRoundFixture.SECOND.getNumber());
            }
        }

        @Nested
        @DisplayName("마지막 차수의 다음 번호보다 큰 번호를 요청한 경우")
        class WhenRoundNumberSkipsRound {

            @Test
            @DisplayName("예외를 던지고 모임을 저장하지 않는다")
            void shouldThrowInvalidGatheringRoundNumberException() {
                // given
                givenGathering(OPEN_PERIOD_OPENED_AT, List.of(first()));

                // when & then
                assertThatThrownBy(() -> roundCommandService.add(command(HOST_MEMBER_ID, 3)))
                        .isInstanceOf(InvalidGatheringRoundNumberException.class);
                thenGatheringNotSaved();
            }
        }

        @Nested
        @DisplayName("차수가 이미 상한까지 있는 경우")
        class WhenRoundCountAtLimit {

            @Test
            @DisplayName("다음 번호를 요청하면 예외를 던지고 모임을 저장하지 않는다")
            void shouldThrowGatheringRoundCountExceedsLimitException() {
                // given
                int limit = GatheringPolicy.MAX_ROUND_COUNT;
                givenGathering(
                        OPEN_PERIOD_OPENED_AT, GatheringRoundFixture.createUpTo(limit));

                // when & then
                assertThatThrownBy(() -> roundCommandService.add(command(HOST_MEMBER_ID, limit + 1)))
                        .isInstanceOf(GatheringRoundCountExceedsLimitException.class);
                thenGatheringNotSaved();
            }
        }

        @Nested
        @DisplayName("번호를 건너뛰면서 상한도 넘는 번호를 요청한 경우")
        class WhenRoundNumberSkipsPastLimit {

            @Test
            @DisplayName("건너뜀보다 상한 초과를 우선해 예외를 던지고 모임을 저장하지 않는다")
            void shouldThrowGatheringRoundCountExceedsLimitException() {
                // given
                int limit = GatheringPolicy.MAX_ROUND_COUNT;
                givenGathering(OPEN_PERIOD_OPENED_AT, List.of(first()));

                // when & then
                assertThatThrownBy(() -> roundCommandService.add(command(HOST_MEMBER_ID, limit + 1)))
                        .isInstanceOf(GatheringRoundCountExceedsLimitException.class);
                thenGatheringNotSaved();
            }
        }
    }

    private AddRoundCommand command(Long memberId, int roundNumber) {
        return new AddRoundCommand(memberId, GATHERING_ID, roundNumber);
    }

    private GatheringRound first() {
        return GatheringRoundFixture.FIRST.create();
    }

    private void givenRetention(GatheringRetentionFixture retention) {
        given(gatheringRetentionRepository.findByGatheringId(GATHERING_ID))
                .willReturn(Optional.of(retention.create(GATHERING_ID, NOW)));
    }

    private void givenGathering(LocalDateTime openedAt, List<GatheringRound> rounds) {
        givenRetention(GatheringRetentionFixture.RETAINED);
        given(gatheringRepository.findById(GATHERING_ID))
                .willReturn(Optional.of(gathering(openedAt, rounds)));
    }

    // 저장은 새로 더한 차수에 id를 채워 돌려준다.
    private void givenSaveAssignsRoundId() {
        given(gatheringRepository.save(any(Gathering.class)))
                .willAnswer(
                        invocation -> {
                            Gathering gathering = invocation.getArgument(0);
                            List<GatheringRound> savedRounds =
                                    gathering.getRounds().stream()
                                            .map(
                                                    round ->
                                                            round.getId() == null
                                                                    ? GatheringRound.builder()
                                                                            .id(SAVED_ROUND_ID)
                                                                            .number(
                                                                                    round
                                                                                            .getNumber())
                                                                            .build()
                                                                    : round)
                                            .toList();

                            return gathering(gathering.getOpenedAt(), savedRounds);
                        });
    }

    private void thenGatheringNotSaved() {
        then(gatheringRepository).should(never()).save(any(Gathering.class));
    }

    private Gathering gathering(LocalDateTime openedAt, List<GatheringRound> rounds) {
        return GatheringFixture.FRIDAY_DINNER.createWithRounds(
                GATHERING_ID, openedAt, List.of(), rounds);
    }
}
