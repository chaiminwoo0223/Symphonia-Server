package com.symphonia.gathering.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.symphonia.IntegrationTest;
import com.symphonia.gathering.application.usecase.GetExpiredGatheringsUseCase;
import com.symphonia.gathering.application.usecase.PurgeGatheringUseCase;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.entity.Participant;
import com.symphonia.gathering.domain.entity.Settlement;
import com.symphonia.gathering.fixture.GatheringFixture;
import com.symphonia.gathering.fixture.GatheringPairingFixture;
import com.symphonia.gathering.fixture.ParticipantFixture;
import com.symphonia.gathering.fixture.SettlementFixture;
import com.symphonia.gathering.helper.GatheringHelper;
import com.symphonia.gathering.helper.SettlementHelper;
import com.symphonia.member.domain.entity.Member;
import com.symphonia.member.fixture.MemberFixture;
import com.symphonia.member.helper.MemberHelper;
import com.symphonia.notification.domain.repository.NotificationDeliveryRepository;
import com.symphonia.notification.domain.repository.NotificationScheduleRepository;
import com.symphonia.notification.fixture.NotificationDeliveryFixture;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

// 실제 테이블과 FK로 두 단계 파기가 모임의 행을 빠짐없이 지우는지 확인한다(#128).
// 파기 시각을 정하는 규칙은 PurgeGatheringServiceTest가 검증하므로, 여기서는 29일이 지난 모임만 쓴다.
@DisplayName("모임 파기 통합 테스트")
class GatheringPurgeIntegrationTest extends IntegrationTest {

    // 테이블마다 모임의 행 수를 센다. 참여자를 참조하는 행은 참여자를 거쳐 모임을 찾는다.
    private static final Map<String, String> COUNT_QUERIES_BY_TABLE =
            Map.ofEntries(
                    Map.entry("gathering", "SELECT COUNT(*) FROM gathering WHERE id = ?"),
                    Map.entry(
                            "gathering_data_key",
                            "SELECT COUNT(*) FROM gathering_data_key WHERE gathering_id = ?"),
                    Map.entry(
                            "gathering_join_token",
                            "SELECT COUNT(*) FROM gathering_join_token WHERE gathering_id = ?"),
                    Map.entry(
                            "participant",
                            "SELECT COUNT(*) FROM participant WHERE gathering_id = ?"),
                    Map.entry(
                            "participant_link_token",
                            "SELECT COUNT(*) FROM participant_link_token WHERE participant_id IN"
                                    + " (SELECT id FROM participant WHERE gathering_id = ?)"),
                    Map.entry(
                            "participant_bank_account",
                            "SELECT COUNT(*) FROM participant_bank_account WHERE participant_id IN"
                                    + " (SELECT id FROM participant WHERE gathering_id = ?)"),
                    Map.entry(
                            "participant_pairing_feedback",
                            "SELECT COUNT(*) FROM participant_pairing_feedback WHERE participant_id IN"
                                    + " (SELECT id FROM participant WHERE gathering_id = ?)"),
                    Map.entry(
                            "gathering_pairing",
                            "SELECT COUNT(*) FROM gathering_pairing WHERE gathering_id = ?"),
                    Map.entry(
                            "settlement", "SELECT COUNT(*) FROM settlement WHERE gathering_id = ?"),
                    Map.entry(
                            "settlement_share",
                            "SELECT COUNT(*) FROM settlement_share WHERE settlement_id IN"
                                    + " (SELECT id FROM settlement WHERE gathering_id = ?)"),
                    Map.entry(
                            "notification_schedule",
                            "SELECT COUNT(*) FROM notification_schedule WHERE gathering_id = ?"),
                    Map.entry(
                            "notification_delivery",
                            "SELECT COUNT(*) FROM notification_delivery WHERE gathering_id = ?"),
                    Map.entry(
                            "notification_delivery_settlement",
                            "SELECT COUNT(*) FROM notification_delivery_settlement WHERE delivery_id"
                                    + " IN (SELECT id FROM notification_delivery WHERE"
                                    + " gathering_id = ?)"));

    @Autowired private GetExpiredGatheringsUseCase getExpiredGatheringsUseCase;
    @Autowired private PurgeGatheringUseCase purgeGatheringUseCase;
    @Autowired private GatheringHelper gatheringHelper;
    @Autowired private SettlementHelper settlementHelper;
    @Autowired private MemberHelper memberHelper;
    @Autowired private NotificationScheduleRepository notificationScheduleRepository;
    @Autowired private NotificationDeliveryRepository notificationDeliveryRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private Long expiredGatheringId;
    private Long retainedGatheringId;

    // 두 모임에 참여 토큰, 개인 링크 토큰, 결제자 계좌, 모임 페어링, 피드백 제출 표시, 정산 건, 발송 예약, 발송 기록을 모두 만든다.
    // 한 모임만 29일 전에 연 모임으로 바꾼다.
    @BeforeEach
    void setUp() {
        Member member = memberHelper.save(MemberFixture.KAKAO);
        expiredGatheringId = saveGatheringWithAllRows(member).getId();
        retainedGatheringId = saveGatheringWithAllRows(member).getId();
        jdbcTemplate.update(
                "UPDATE gathering SET opened_at = ? WHERE id = ?",
                LocalDateTime.now().minusDays(29),
                expiredGatheringId);
    }

    @Nested
    @DisplayName("getExpiredGatheringIds 메서드는")
    class GetExpiredGatheringIds {

        @Test
        @DisplayName("파기 시각이 지난 모임만 찾는다")
        void shouldFindOnlyExpiredGatherings() {
            // when
            List<Long> result = getExpiredGatheringsUseCase.getExpiredGatheringIds();

            // then
            assertThat(result).contains(expiredGatheringId).doesNotContain(retainedGatheringId);
        }
    }

    @Nested
    @DisplayName("discardDataKeyIfExpired 메서드는")
    class DiscardDataKeyIfExpired {

        @Nested
        @DisplayName("파기 시각이 지난 모임인 경우")
        class WhenGatheringExpired {

            @Test
            @DisplayName("데이터 키만 지우고 true를 반환한다")
            void shouldDiscardOnlyDataKey() {
                // when
                boolean result = purgeGatheringUseCase.discardDataKeyIfExpired(expiredGatheringId);

                // then
                assertThat(result).isTrue();
                assertThat(countRows("gathering_data_key", expiredGatheringId)).isZero();
                assertThat(countRows("participant", expiredGatheringId)).isPositive();
            }
        }

        @Nested
        @DisplayName("파기 시각이 남은 모임인 경우")
        class WhenGatheringRetained {

            @Test
            @DisplayName("데이터 키를 지우지 않고 false를 반환한다")
            void shouldKeepDataKey() {
                // when
                boolean result = purgeGatheringUseCase.discardDataKeyIfExpired(retainedGatheringId);

                // then
                assertThat(result).isFalse();
                assertThat(countRows("gathering_data_key", retainedGatheringId)).isOne();
            }
        }
    }

    @Nested
    @DisplayName("purge 메서드는")
    class Purge {

        @Test
        @DisplayName("데이터 키를 지운 모임의 행을 모든 테이블에서 지우고 다른 모임의 행은 남긴다")
        void shouldDeleteAllRowsOfPurgedGatheringOnly() {
            // given: 파기 1단계로 데이터 키를 지운 상태
            jdbcTemplate.update(
                    "DELETE FROM gathering_data_key WHERE gathering_id = ?", expiredGatheringId);

            // when
            purgeGatheringUseCase.purge(expiredGatheringId);

            // then
            COUNT_QUERIES_BY_TABLE.keySet().stream()
                    .filter(table -> !table.equals("gathering_data_key"))
                    .forEach(
                            table -> {
                                assertThat(countRows(table, expiredGatheringId))
                                        .as(table + "의 파기한 모임 행")
                                        .isZero();
                                assertThat(countRows(table, retainedGatheringId))
                                        .as(table + "의 다른 모임 행")
                                        .isPositive();
                            });
        }
    }

    private Gathering saveGatheringWithAllRows(Member member) {
        Gathering gathering =
                gatheringHelper.join(
                        gatheringHelper.save(GatheringFixture.FRIDAY_DINNER, member.getId()),
                        ParticipantFixture.KIM,
                        ParticipantFixture.LEE,
                        ParticipantFixture.PARK);
        Participant payer = gathering.getParticipants().get(0);
        Participant lee = gathering.getParticipants().get(1);
        gatheringHelper.issueJoinToken(gathering);
        gatheringHelper.issueLinkToken(lee);
        settlementHelper.registerBankAccount(gathering, payer);
        gatheringHelper.choosePairing(gathering, GatheringPairingFixture.FRIEND_CASUAL.create());
        gatheringHelper.markPairingFeedbackSubmitted(lee);
        Settlement settlement = settlementHelper.record(SettlementFixture.DINNER, gathering, payer);
        notificationScheduleRepository.findOrCreateForUpdate(gathering.getId());
        notificationDeliveryRepository.save(
                NotificationDeliveryFixture.SETTLEMENT_NOTICE.create(
                        gathering.getId(), lee.getId(), List.of(settlement.getId())));

        return gathering;
    }

    private long countRows(String table, Long gatheringId) {
        return jdbcTemplate.queryForObject(
                COUNT_QUERIES_BY_TABLE.get(table), Long.class, gatheringId);
    }
}
