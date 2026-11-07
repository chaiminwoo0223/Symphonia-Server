package com.symphonia.gathering.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.symphonia.UnitTest;
import com.symphonia.gathering.application.dto.result.LinkedParticipantResult;
import com.symphonia.gathering.domain.exception.LinkTokenNotFoundException;
import com.symphonia.gathering.domain.repository.GatheringRetentionRepository;
import com.symphonia.gathering.domain.repository.ParticipantLinkTokenRepository;
import com.symphonia.gathering.fixture.GatheringRetentionFixture;
import com.symphonia.global.config.TimeConfig;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

@DisplayName("ParticipantQueryService 단위 테스트")
class ParticipantQueryServiceTest extends UnitTest {

    private static final Long GATHERING_ID = 10L;
    private static final Long PARTICIPANT_ID = 20L;
    private static final String LINK_TOKEN = "link-token";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 29, 18, 0);

    private ParticipantQueryService participantQueryService;

    @Mock private ParticipantLinkTokenRepository participantLinkTokenRepository;
    @Mock private GatheringRetentionRepository gatheringRetentionRepository;

    @BeforeEach
    void setUp() {
        participantQueryService =
                new ParticipantQueryService(
                        participantLinkTokenRepository,
                        gatheringRetentionRepository,
                        Clock.fixed(
                                NOW.atZone(TimeConfig.SERVICE_ZONE).toInstant(),
                                TimeConfig.SERVICE_ZONE));
    }

    @Nested
    @DisplayName("getLinkedParticipant 메서드는")
    class GetLinkedParticipant {

        @Nested
        @DisplayName("개인 링크 토큰에 해당하는 참여자가 없는 경우")
        class WhenLinkTokenNotFound {

            @Test
            @DisplayName("예외를 던지고 파기 시각을 읽지 않는다")
            void shouldThrowLinkTokenNotFoundException() {
                // given
                given(participantLinkTokenRepository.findParticipantIdByValue(LINK_TOKEN))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> participantQueryService.getLinkedParticipant(LINK_TOKEN))
                        .isInstanceOf(LinkTokenNotFoundException.class);
                then(gatheringRetentionRepository).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("참여자가 속한 모임의 파기 시각이 없는 경우")
        class WhenRetentionNotFound {

            @Test
            @DisplayName("물리 삭제 뒤와 같게 링크 토큰을 찾지 못한 예외를 던진다")
            void shouldThrowLinkTokenNotFoundException() {
                // given
                given(participantLinkTokenRepository.findParticipantIdByValue(LINK_TOKEN))
                        .willReturn(Optional.of(PARTICIPANT_ID));
                given(gatheringRetentionRepository.findByParticipantId(PARTICIPANT_ID))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> participantQueryService.getLinkedParticipant(LINK_TOKEN))
                        .isInstanceOf(LinkTokenNotFoundException.class);
            }
        }

        @Nested
        @DisplayName("모임의 파기 시각이 된 경우")
        class WhenGatheringExpired {

            @Test
            @DisplayName("물리 삭제 전이라도 링크 토큰을 찾지 못한 예외를 던진다")
            void shouldThrowLinkTokenNotFoundException() {
                // given
                givenRetention(GatheringRetentionFixture.EXPIRED);

                // when & then
                assertThatThrownBy(() -> participantQueryService.getLinkedParticipant(LINK_TOKEN))
                        .isInstanceOf(LinkTokenNotFoundException.class);
            }
        }

        @Nested
        @DisplayName("모임의 데이터 키를 지운 경우")
        class WhenDataKeyDiscarded {

            @Test
            @DisplayName("파기 시각이 남았어도 링크 토큰을 찾지 못한 예외를 던진다")
            void shouldThrowLinkTokenNotFoundException() {
                // given
                givenRetention(GatheringRetentionFixture.DATA_KEY_DISCARDED);

                // when & then
                assertThatThrownBy(() -> participantQueryService.getLinkedParticipant(LINK_TOKEN))
                        .isInstanceOf(LinkTokenNotFoundException.class);
            }
        }

        @Nested
        @DisplayName("모임의 파기 시각이 남은 경우")
        class WhenGatheringRetained {

            @Test
            @DisplayName("참여자 id와 모임의 파기 시각을 반환한다")
            void shouldReturnParticipantIdAndDeletesAt() {
                // given
                givenRetention(GatheringRetentionFixture.RETAINED);

                // when
                LinkedParticipantResult result =
                        participantQueryService.getLinkedParticipant(LINK_TOKEN);

                // then
                assertThat(result)
                        .isEqualTo(
                                new LinkedParticipantResult(
                                        PARTICIPANT_ID,
                                        GatheringRetentionFixture.RETAINED
                                                .create(GATHERING_ID, NOW)
                                                .deletesAt()));
            }

            @Test
            @DisplayName("파기 시각 1초 전이면 아직 파기하지 않은 것으로 보고 참여자 id를 반환한다")
            void shouldReturnParticipantIdWhenOneSecondBeforeDeletesAt() {
                // given
                givenRetention(GatheringRetentionFixture.ABOUT_TO_EXPIRE);

                // when
                LinkedParticipantResult result =
                        participantQueryService.getLinkedParticipant(LINK_TOKEN);

                // then
                assertThat(result.participantId()).isEqualTo(PARTICIPANT_ID);
                assertThat(result.deletesAt()).isEqualTo(NOW.plusSeconds(1));
            }
        }

        private void givenRetention(GatheringRetentionFixture retention) {
            given(participantLinkTokenRepository.findParticipantIdByValue(LINK_TOKEN))
                    .willReturn(Optional.of(PARTICIPANT_ID));
            given(gatheringRetentionRepository.findByParticipantId(PARTICIPANT_ID))
                    .willReturn(Optional.of(retention.create(GATHERING_ID, NOW)));
        }
    }
}
