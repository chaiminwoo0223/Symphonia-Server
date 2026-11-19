package com.symphonia.gathering.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;

import com.symphonia.UnitTest;
import com.symphonia.gathering.application.dto.command.ReissueParticipantLinkTokenCommand;
import com.symphonia.gathering.application.dto.command.RemoveParticipantCommand;
import com.symphonia.gathering.application.dto.result.ParticipantResult;
import com.symphonia.gathering.domain.exception.GatheringNotFoundException;
import com.symphonia.gathering.domain.exception.NotGatheringHostException;
import com.symphonia.gathering.domain.exception.ParticipantInSettlementException;
import com.symphonia.gathering.domain.exception.ParticipantNotFoundException;
import com.symphonia.gathering.domain.provider.GatheringTokenProvider;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.domain.repository.GatheringRetentionRepository;
import com.symphonia.gathering.domain.repository.ParticipantBankAccountRepository;
import com.symphonia.gathering.domain.repository.ParticipantLinkTokenRepository;
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
import org.mockito.InOrder;
import org.mockito.Mock;

@DisplayName("ParticipantCommandService 단위 테스트")
class ParticipantCommandServiceTest extends UnitTest {

    private static final Long GATHERING_ID = 10L;
    private static final Long PARTICIPANT_ID = 20L;
    private static final Long OTHER_PARTICIPANT_ID = 21L;
    private static final Long OTHER_MEMBER_ID = 99L;
    private static final Long HOST_MEMBER_ID = GatheringFixture.FRIDAY_DINNER.getHostMemberId();
    private static final String NEW_LINK_TOKEN = "new-link-token";
    private static final String MASKED_PHONE_NUMBER = "010-****-5678";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 19, 18, 0);

    // 기록 기간(48시간)이 지나도 링크를 잃은 참여자는 분담금을 보내야 하므로, 닫힌 뒤의 모임으로 경계를 확인한다.
    private static final LocalDateTime OPEN_PERIOD_OPENED_AT = NOW.minusHours(48).plusSeconds(1);
    private static final LocalDateTime CLOSED_PERIOD_OPENED_AT = NOW.minusHours(48);

    private ParticipantCommandService participantCommandService;

    @Mock private GatheringRepository gatheringRepository;
    @Mock private GatheringRetentionRepository gatheringRetentionRepository;
    @Mock private ParticipantLinkTokenRepository participantLinkTokenRepository;
    @Mock private ParticipantBankAccountRepository participantBankAccountRepository;
    @Mock private SettlementRepository settlementRepository;
    @Mock private GatheringTokenProvider gatheringTokenProvider;

    @BeforeEach
    void setUp() {
        participantCommandService =
                new ParticipantCommandService(
                        gatheringRepository,
                        gatheringRetentionRepository,
                        participantLinkTokenRepository,
                        participantBankAccountRepository,
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
        @DisplayName("정산 건의 결제자이거나 인원인 참여자인 경우")
        class WhenParticipantInSettlement {

            @Test
            @DisplayName("예외를 던지고 아무것도 지우지 않는다")
            void shouldThrowParticipantInSettlementException() {
                // given
                givenGatheringWithParticipant(OPEN_PERIOD_OPENED_AT);
                given(settlementRepository.existsByParticipantId(PARTICIPANT_ID)).willReturn(true);

                // when & then
                assertThatThrownBy(() -> participantCommandService.remove(command(HOST_MEMBER_ID)))
                        .isInstanceOf(ParticipantInSettlementException.class);
                thenNothingDeleted();
            }
        }

        @Nested
        @DisplayName("정산 건에 없는 참여자인 경우")
        class WhenParticipantRemovable {

            @Test
            @DisplayName("개인 링크 토큰, 계좌, 참여자 순서로 지운다")
            void shouldDeleteReferencesBeforeParticipant() {
                // given
                givenGatheringWithParticipant(OPEN_PERIOD_OPENED_AT);
                given(settlementRepository.existsByParticipantId(PARTICIPANT_ID)).willReturn(false);

                // when
                participantCommandService.remove(command(HOST_MEMBER_ID));

                // then
                InOrder inOrder =
                        inOrder(
                                participantLinkTokenRepository,
                                participantBankAccountRepository,
                                gatheringRepository);
                then(participantLinkTokenRepository)
                        .should(inOrder)
                        .deleteAllByParticipantId(PARTICIPANT_ID);
                then(participantBankAccountRepository)
                        .should(inOrder)
                        .deleteByParticipantId(PARTICIPANT_ID);
                then(gatheringRepository).should(inOrder).deleteParticipant(PARTICIPANT_ID);
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
                given(settlementRepository.existsByParticipantId(PARTICIPANT_ID)).willReturn(false);

                // when
                participantCommandService.remove(command(HOST_MEMBER_ID));

                // then
                then(gatheringRepository).should().deleteParticipant(PARTICIPANT_ID);
            }
        }

        private RemoveParticipantCommand command(Long memberId) {
            return RemoveParticipantCommand.of(memberId, GATHERING_ID, PARTICIPANT_ID);
        }

        private void thenNothingDeleted() {
            then(participantLinkTokenRepository).shouldHaveNoInteractions();
            then(participantBankAccountRepository).shouldHaveNoInteractions();
            then(gatheringRepository).should(never()).deleteParticipant(anyLong());
        }
    }

    private void givenGatheringWithParticipant(LocalDateTime openedAt) {
        givenRetention(GatheringRetentionFixture.RETAINED);
        given(gatheringRepository.findById(GATHERING_ID))
                .willReturn(
                        Optional.of(
                                GatheringFixture.FRIDAY_DINNER.createWithIdAndOpenedAt(
                                        GATHERING_ID,
                                        openedAt,
                                        List.of(
                                                ParticipantFixture.KIM.createWithId(
                                                        PARTICIPANT_ID)))));
    }

    private void givenRetention(GatheringRetentionFixture retention) {
        given(gatheringRetentionRepository.findByGatheringId(GATHERING_ID))
                .willReturn(Optional.of(retention.create(GATHERING_ID, NOW)));
    }
}
