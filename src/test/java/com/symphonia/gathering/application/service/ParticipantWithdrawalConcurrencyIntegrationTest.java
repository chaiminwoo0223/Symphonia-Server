package com.symphonia.gathering.application.service;

import static com.symphonia.common.helper.ConcurrencyHelper.runConcurrently;
import static org.assertj.core.api.Assertions.assertThat;

import com.symphonia.IntegrationTest;
import com.symphonia.gathering.application.dto.command.RecordSettlementCommand;
import com.symphonia.gathering.application.dto.command.RemoveParticipantBySubjectCommand;
import com.symphonia.gathering.application.dto.command.RemoveParticipantCommand;
import com.symphonia.gathering.application.usecase.RecordSettlementUseCase;
import com.symphonia.gathering.application.usecase.RemoveParticipantBySubjectUseCase;
import com.symphonia.gathering.application.usecase.RemoveParticipantUseCase;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.entity.Participant;
import com.symphonia.gathering.domain.exception.InvalidSettlementParticipantsException;
import com.symphonia.gathering.domain.exception.ParticipantUnremovableException;
import com.symphonia.gathering.domain.repository.ParticipantGoogleSubjectRepository;
import com.symphonia.gathering.fixture.GatheringFixture;
import com.symphonia.gathering.fixture.ParticipantFixture;
import com.symphonia.gathering.fixture.SettlementFixture;
import com.symphonia.gathering.helper.GatheringHelper;
import com.symphonia.gathering.helper.SettlementHelper;
import com.symphonia.member.domain.entity.Member;
import com.symphonia.member.domain.repository.MemberRepository;
import com.symphonia.member.fixture.MemberFixture;
import com.symphonia.member.helper.MemberHelper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

// 참여자의 철회와 모임장의 제거는 모임 행을 잠가, 같은 참여자를 인원으로 넣는 정산 건 기록과 차례로 처리돼야 한다(#221).
// 잠그지 않으면 취소하지 않은 정산 건이 익명화한 참여자를 가리킬 수 있다.
// 트랜잭션 롤백 없이 커밋하므로 직접 지운다.
@DisplayName("참여자 철회와 제거 동시성 통합 테스트")
class ParticipantWithdrawalConcurrencyIntegrationTest extends IntegrationTest {

    private static final String GOOGLE_SUBJECT = "109876543210987654321";

    @Autowired private RemoveParticipantBySubjectUseCase removeParticipantBySubjectUseCase;
    @Autowired private RemoveParticipantUseCase removeParticipantUseCase;
    @Autowired private RecordSettlementUseCase recordSettlementUseCase;
    @Autowired private ParticipantGoogleSubjectRepository participantGoogleSubjectRepository;
    @Autowired private GatheringHelper gatheringHelper;
    @Autowired private SettlementHelper settlementHelper;
    @Autowired private MemberHelper memberHelper;
    @Autowired private MemberRepository memberRepository;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;

    private Member host;
    private Gathering gathering;
    private Participant payer;
    private Participant leaver;
    private String joinToken;

    // 저장소는 부른 쪽의 트랜잭션을 전제로 하므로, 테스트 트랜잭션 없이 도는 준비도 트랜잭션 안에서 저장한다.
    // 떠나는 참여자는 취소한 정산 건에만 들어 있어, 철회와 제거가 행을 지우지 않고 익명화하는 상태다.
    @BeforeEach
    void saveGathering() {
        host = memberHelper.save(MemberFixture.KAKAO);
        transactionTemplate.executeWithoutResult(
                _ -> {
                    gathering =
                            gatheringHelper.join(
                                    gatheringHelper.save(
                                            GatheringFixture.FRIDAY_DINNER, host.getId()),
                                    ParticipantFixture.KIM,
                                    ParticipantFixture.LEE);
                    payer = gathering.getParticipants().get(0);
                    leaver = gathering.getParticipants().get(1);
                    participantGoogleSubjectRepository.save(
                            gathering.getId(), leaver.getId(), GOOGLE_SUBJECT);
                    settlementHelper.cancel(
                            settlementHelper.record(SettlementFixture.DINNER, gathering, payer));
                    joinToken = gatheringHelper.issueJoinToken(gathering);
                });
    }

    @AfterEach
    void deleteCommittedRows() {
        gatheringHelper.deleteRows(gathering.getId());
        memberRepository.delete(host);
    }

    @Nested
    @DisplayName("remove 메서드(참여 토큰과 구글 계정)와 record 메서드는")
    class WithdrawAndRecord {

        @Test
        @Transactional(propagation = Propagation.NOT_SUPPORTED)
        @DisplayName("겹쳐 실행돼도 한 요청만 성공하고 취소하지 않은 정산 건이 철회한 참여자를 가리키지 않는다")
        void shouldKeepSettlementAwayFromWithdrawnParticipantWhenRunConcurrently() {
            // when
            List<Throwable> failures =
                    runConcurrently(
                            () ->
                                    removeParticipantBySubjectUseCase.remove(
                                            new RemoveParticipantBySubjectCommand(
                                                    joinToken, GOOGLE_SUBJECT)),
                            () -> recordSettlementUseCase.record(recordCommand()));

            // then
            assertOneRequestWonAndStateConsistent(failures);
        }
    }

    @Nested
    @DisplayName("remove 메서드(모임장)와 record 메서드는")
    class RemoveAndRecord {

        @Test
        @Transactional(propagation = Propagation.NOT_SUPPORTED)
        @DisplayName("겹쳐 실행돼도 한 요청만 성공하고 취소하지 않은 정산 건이 제거한 참여자를 가리키지 않는다")
        void shouldKeepSettlementAwayFromRemovedParticipantWhenRunConcurrently() {
            // when
            List<Throwable> failures =
                    runConcurrently(
                            () ->
                                    removeParticipantUseCase.remove(
                                            RemoveParticipantCommand.of(
                                                    host.getId(),
                                                    gathering.getId(),
                                                    leaver.getId())),
                            () -> recordSettlementUseCase.record(recordCommand()));

            // then
            assertOneRequestWonAndStateConsistent(failures);
        }
    }

    // 먼저 잠금을 얻은 요청이 이기고, 늦은 요청이 정확히 하나만 실패한다.
    // 철회나 제거가 먼저면 기록이 인원 없음으로, 기록이 먼저면 철회나 제거가 PARTICIPANT_UNREMOVABLE로 실패한다.
    private void assertOneRequestWonAndStateConsistent(List<Throwable> failures) {
        assertThat(failures).hasSize(1);
        assertThat(failures.getFirst())
                .isInstanceOfAny(
                        ParticipantUnremovableException.class,
                        InvalidSettlementParticipantsException.class);
        assertThat(uncancelledSettlementCount() + withdrawnParticipantCount()).isOne();
        assertThat(uncancelledSettlementsOfWithdrawnParticipantCount()).isZero();
    }

    private RecordSettlementCommand recordCommand() {
        return new RecordSettlementCommand(
                host.getId(),
                gathering.getId(),
                payer.getId(),
                UUID.randomUUID(),
                gathering.getRounds().getFirst().getId(),
                SettlementFixture.DINNER.getTotalAmount(),
                List.of(payer.getId(), leaver.getId()),
                Map.of());
    }

    private Long uncancelledSettlementCount() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM settlement WHERE gathering_id = ? AND cancelled_at IS NULL",
                Long.class,
                gathering.getId());
    }

    private Long withdrawnParticipantCount() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM participant WHERE gathering_id = ? AND withdrawn_at IS NOT NULL",
                Long.class,
                gathering.getId());
    }

    // 결제자이거나 분담금 인원인 참여자가 익명화돼 있는 취소하지 않은 정산 건의 수다.
    private Long uncancelledSettlementsOfWithdrawnParticipantCount() {
        return jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM settlement s
                WHERE s.gathering_id = ? AND s.cancelled_at IS NULL
                  AND (EXISTS (SELECT 1 FROM participant p
                               WHERE p.id = s.payer_participant_id AND p.withdrawn_at IS NOT NULL)
                       OR EXISTS (SELECT 1 FROM settlement_share sh
                                  JOIN participant p ON p.id = sh.participant_id
                                  WHERE sh.settlement_id = s.id AND p.withdrawn_at IS NOT NULL))
                """,
                Long.class,
                gathering.getId());
    }
}
