package com.symphonia.gathering.application.service;

import static com.symphonia.common.helper.ConcurrencyHelper.runConcurrently;
import static org.assertj.core.api.Assertions.assertThat;

import com.symphonia.IntegrationTest;
import com.symphonia.gathering.application.dto.command.SubmitParticipantPairingFeedbackCommand;
import com.symphonia.gathering.application.usecase.SubmitParticipantPairingFeedbackUseCase;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.entity.Participant;
import com.symphonia.gathering.domain.exception.PairingFeedbackAlreadySubmittedException;
import com.symphonia.gathering.domain.vo.GatheringPairing;
import com.symphonia.gathering.fixture.GatheringFixture;
import com.symphonia.gathering.fixture.GatheringPairingFixture;
import com.symphonia.gathering.fixture.ParticipantFixture;
import com.symphonia.gathering.fixture.SettlementFixture;
import com.symphonia.gathering.helper.GatheringHelper;
import com.symphonia.gathering.helper.SettlementHelper;
import com.symphonia.member.domain.entity.Member;
import com.symphonia.member.domain.repository.MemberRepository;
import com.symphonia.member.fixture.MemberFixture;
import com.symphonia.member.helper.MemberHelper;
import com.symphonia.pairing.domain.vo.PairingRating;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

// 같은 참여자가 피드백을 연달아 눌러 같은 시점에 들어오는 경우에도 피드백이 한 건만 쌓이는지 실제 DB로 확인한다(#129).
// 피드백을 내려면 카탈로그에 실제 음료와 안주가 있어야 하므로 시드로 적재된 첫 음료와 안주를 쓴다. 트랜잭션 롤백 없이 커밋하므로 직접 지운다.
@DisplayName("참여자 페어링 피드백 동시성 통합 테스트")
class ParticipantPairingFeedbackConcurrencyIntegrationTest extends IntegrationTest {

    private static final int CONCURRENT_SUBMISSIONS = 4;

    @Autowired private SubmitParticipantPairingFeedbackUseCase submitFeedbackUseCase;
    @Autowired private GatheringHelper gatheringHelper;
    @Autowired private SettlementHelper settlementHelper;
    @Autowired private MemberHelper memberHelper;
    @Autowired private MemberRepository memberRepository;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;

    private Member host;
    private Gathering gathering;
    private Participant participant;
    private String linkToken;
    private Long drinkId;
    private Long anjuId;
    private GatheringPairing pairing;

    // 이가 결제자인 정산 건에서 김이 몫을 보냈고, 모임장이 고른 페어링이 있는 모임이다.
    // 저장소는 부른 쪽의 트랜잭션을 전제로 하므로, 테스트 트랜잭션 없이 도는 준비도 트랜잭션 안에서 저장한다.
    @BeforeEach
    void setUp() {
        host = memberHelper.save(MemberFixture.KAKAO);
        drinkId = jdbcTemplate.queryForObject("SELECT MIN(id) FROM drink", Long.class);
        anjuId = jdbcTemplate.queryForObject("SELECT MIN(id) FROM anju", Long.class);
        pairing = GatheringPairingFixture.FRIEND_CASUAL.createWithReferences(drinkId, anjuId);
        transactionTemplate.executeWithoutResult(
                _ -> {
                    gathering =
                            gatheringHelper.join(
                                    gatheringHelper.save(
                                            GatheringFixture.FRIDAY_DINNER, host.getId()),
                                    ParticipantFixture.KIM,
                                    ParticipantFixture.LEE);
                    participant = gathering.getParticipants().get(0);
                    Participant payer = gathering.getParticipants().get(1);
                    linkToken = gatheringHelper.issueLinkToken(participant);
                    gatheringHelper.choosePairing(gathering, pairing);
                    settlementHelper.markSent(
                            settlementHelper.record(SettlementFixture.DINNER, gathering, payer),
                            participant);
                });
    }

    @AfterEach
    void deleteCommittedRows() {
        gatheringHelper.deleteRows(gathering.getId());
        memberRepository.delete(host);
        jdbcTemplate.update(
                "DELETE FROM pairing_rating_summary WHERE drink_id = ? AND anju_id = ?",
                drinkId,
                anjuId);
        jdbcTemplate.update(
                "DELETE FROM pairing_feedback WHERE drink_id = ? AND anju_id = ?", drinkId, anjuId);
    }

    @Nested
    @DisplayName("submit 메서드는")
    class Submit {

        @Test
        @Transactional(propagation = Propagation.NOT_SUPPORTED)
        @DisplayName(
                "같은 참여자가 동시에 제출해도 한 건만 받고 나머지는 PairingFeedbackAlreadySubmittedException으로 거절한다")
        void shouldAcceptOnlyOneWhenSubmittedConcurrentlyBySameParticipant() {
            // given
            List<Runnable> submissions =
                    IntStream.range(0, CONCURRENT_SUBMISSIONS)
                            .<Runnable>mapToObj(
                                    _ -> () -> submitFeedbackUseCase.submit(submitCommand()))
                            .toList();

            // when
            List<Throwable> failures = runConcurrently(submissions.toArray(Runnable[]::new));

            // then
            assertThat(failures)
                    .hasSize(CONCURRENT_SUBMISSIONS - 1)
                    .allSatisfy(
                            failure ->
                                    assertThat(failure)
                                            .isInstanceOf(
                                                    PairingFeedbackAlreadySubmittedException
                                                            .class));
            assertThat(countFeedbacks()).isOne();
            assertThat(countSubmissionMarks()).isOne();
        }
    }

    private SubmitParticipantPairingFeedbackCommand submitCommand() {
        return new SubmitParticipantPairingFeedbackCommand(
                linkToken, pairing.drinkId(), pairing.anjuId(), PairingRating.LIKE);
    }

    private Long countFeedbacks() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pairing_feedback WHERE drink_id = ? AND anju_id = ?",
                Long.class,
                drinkId,
                anjuId);
    }

    private Long countSubmissionMarks() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM participant_pairing_feedback WHERE participant_id = ?",
                Long.class,
                participant.getId());
    }
}
