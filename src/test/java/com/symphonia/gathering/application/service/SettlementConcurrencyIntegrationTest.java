package com.symphonia.gathering.application.service;

import static com.symphonia.common.helper.ConcurrencyHelper.runConcurrently;
import static org.assertj.core.api.Assertions.assertThat;

import com.symphonia.IntegrationTest;
import com.symphonia.gathering.application.dto.command.MarkSharesSentCommand;
import com.symphonia.gathering.application.dto.command.RecordSettlementCommand;
import com.symphonia.gathering.application.dto.command.UpdateSettlementCommand;
import com.symphonia.gathering.application.dto.result.SettlementResult;
import com.symphonia.gathering.application.usecase.MarkSharesSentUseCase;
import com.symphonia.gathering.application.usecase.RecordSettlementUseCase;
import com.symphonia.gathering.application.usecase.UpdateSettlementUseCase;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.entity.Participant;
import com.symphonia.gathering.domain.entity.Settlement;
import com.symphonia.gathering.domain.entity.SettlementShare;
import com.symphonia.gathering.domain.exception.SettlementConflictException;
import com.symphonia.gathering.domain.exception.SettlementSharesSentException;
import com.symphonia.gathering.domain.exception.UnpaidShareConflictException;
import com.symphonia.gathering.domain.repository.SettlementRepository;
import com.symphonia.gathering.domain.vo.PhoneNumber;
import com.symphonia.gathering.domain.vo.ShareStatus;
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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

// 요청이 Service와 트랜잭션을 거쳐 동시에 들어올 때 정산 건이 한 건이고 최종 상태가 일관되는지 실제 DB로 확인한다(#129).
// 트랜잭션 롤백 없이 커밋하므로 직접 지운다.
@DisplayName("정산 건 동시성 통합 테스트")
class SettlementConcurrencyIntegrationTest extends IntegrationTest {

    private static final int CONCURRENT_RECORDS = 4;

    @Autowired private RecordSettlementUseCase recordSettlementUseCase;
    @Autowired private UpdateSettlementUseCase updateSettlementUseCase;
    @Autowired private MarkSharesSentUseCase markSharesSentUseCase;
    @Autowired private SettlementRepository settlementRepository;
    @Autowired private GatheringHelper gatheringHelper;
    @Autowired private SettlementHelper settlementHelper;
    @Autowired private MemberHelper memberHelper;
    @Autowired private MemberRepository memberRepository;
    @Autowired private TransactionTemplate transactionTemplate;

    private Member host;
    private Gathering gathering;
    private Participant payer;
    private Participant sender;

    // 저장소는 부른 쪽의 트랜잭션을 전제로 하므로, 테스트 트랜잭션 없이 도는 준비도 트랜잭션 안에서 저장한다.
    @BeforeEach
    void saveGathering() {
        host = memberHelper.save(MemberFixture.KAKAO);
        gathering =
                transactionTemplate.execute(
                        _ ->
                                gatheringHelper.join(
                                        gatheringHelper.save(
                                                GatheringFixture.FRIDAY_DINNER, host.getId()),
                                        ParticipantFixture.KIM,
                                        ParticipantFixture.LEE,
                                        ParticipantFixture.PARK));
        payer = participantOf(ParticipantFixture.KIM);
        sender = participantOf(ParticipantFixture.LEE);
    }

    @AfterEach
    void deleteCommittedRows() {
        gatheringHelper.deleteRows(gathering.getId());
        memberRepository.delete(host);
    }

    @Nested
    @DisplayName("record 메서드는")
    class RecordSettlement {

        @Test
        @Transactional(propagation = Propagation.NOT_SUPPORTED)
        @DisplayName("같은 요청 id로 동시에 기록해도 정산 건을 한 건만 만들고 모두 그 정산 건을 돌려준다")
        void shouldRecordOnceWhenRecordedConcurrentlyWithSameRequestId() {
            // given
            UUID clientRequestId = UUID.randomUUID();
            List<SettlementResult> results = new CopyOnWriteArrayList<>();
            List<Runnable> records =
                    IntStream.range(0, CONCURRENT_RECORDS)
                            .<Runnable>mapToObj(
                                    _ ->
                                            () ->
                                                    results.add(
                                                            recordSettlementUseCase.record(
                                                                    recordCommand(
                                                                            clientRequestId))))
                            .toList();

            // when
            List<Throwable> failures = runConcurrently(records.toArray(Runnable[]::new));

            // then
            assertThat(failures).isEmpty();
            assertThat(results).hasSize(CONCURRENT_RECORDS);
            assertThat(results).extracting(SettlementResult::id).containsOnly(recordedId());
            assertThat(settlementsOfGathering()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("markSent 메서드는")
    class MarkSent {

        @Test
        @Transactional(propagation = Propagation.NOT_SUPPORTED)
        @DisplayName("같은 정산 건의 참여자들이 동시에 눌러도 모두 성공하고 분담금이 모두 보냄이 된다")
        void shouldMarkAllSharesSentWhenMarkedConcurrently() {
            // given
            Settlement recorded =
                    transactionTemplate.execute(
                            _ ->
                                    settlementHelper.record(
                                            SettlementFixture.DINNER, gathering, payer));
            List<Runnable> marks =
                    Stream.of(sender, participantOf(ParticipantFixture.PARK))
                            .map(
                                    participant ->
                                            transactionTemplate.execute(
                                                    _ ->
                                                            gatheringHelper.issueLinkToken(
                                                                    participant)))
                            .<Runnable>map(
                                    linkToken ->
                                            () ->
                                                    markSharesSentUseCase.markSent(
                                                            new MarkSharesSentCommand(
                                                                    linkToken,
                                                                    payer.getId(),
                                                                    seenAmounts(recorded))))
                            .toList();

            // when
            List<Throwable> failures = runConcurrently(marks.toArray(Runnable[]::new));

            // then
            assertThat(failures).isEmpty();
            assertThat(sentShareParticipantIds(findSettlement(recorded.getId())))
                    .containsExactlyInAnyOrder(
                            sender.getId(), participantOf(ParticipantFixture.PARK).getId());
        }
    }

    @Nested
    @DisplayName("update와 markSent 메서드는")
    class UpdateAndMarkSent {

        @Test
        @Transactional(propagation = Propagation.NOT_SUPPORTED)
        @DisplayName("겹쳐 실행돼도 어느 순서든 분담금 상태와 금액이 서로 어긋나지 않는다")
        void shouldKeepSharesConsistentWhenRunConcurrently() {
            // given
            Settlement recorded =
                    transactionTemplate.execute(
                            _ ->
                                    settlementHelper.record(
                                            SettlementFixture.DINNER, gathering, payer));
            String linkToken =
                    transactionTemplate.execute(_ -> gatheringHelper.issueLinkToken(sender));

            Map<Long, Long> seenAmounts = seenAmounts(recorded);

            AtomicBoolean updated = new AtomicBoolean();
            AtomicBoolean marked = new AtomicBoolean();

            // when
            List<Throwable> failures =
                    runConcurrently(
                            () -> {
                                updateSettlementUseCase.update(
                                        updateCommand(
                                                recorded.getId(), SettlementFixture.SECOND_ROUND));
                                updated.set(true);
                            },
                            () -> {
                                markSharesSentUseCase.markSent(
                                        new MarkSharesSentCommand(
                                                linkToken, payer.getId(), seenAmounts));
                                marked.set(true);
                            });

            // then
            // 늦은 수정은 보낸 분담금이 있다는 거절이나 버전 충돌로 실패하고, 늦은 보냈어요는 본 금액이 달라 거절된다.
            // 어느 요청이 이겼는지와 관계없이 하나만 성공하고, 최종 상태는 성공한 요청의 결과와 정확히 일치해야 한다.
            assertThat(failures)
                    .allSatisfy(
                            failure ->
                                    assertThat(failure)
                                            .isInstanceOfAny(
                                                    SettlementSharesSentException.class,
                                                    SettlementConflictException.class,
                                                    UnpaidShareConflictException.class));
            assertThat(updated.get() ^ marked.get()).isTrue();
            Settlement reloaded = findSettlement(recorded.getId());
            SettlementFixture expected =
                    updated.get() ? SettlementFixture.SECOND_ROUND : SettlementFixture.DINNER;
            assertThat(reloaded.getTotalAmount()).isEqualTo(expected.getTotalAmount());
            assertThat(shareAmounts(reloaded)).isEqualTo(expectedShareAmounts(expected));
            assertThat(sentShareParticipantIds(reloaded))
                    .isEqualTo(marked.get() ? List.of(sender.getId()) : List.of());
        }
    }

    private Participant participantOf(ParticipantFixture fixture) {
        return gathering.getParticipant(new PhoneNumber(fixture.getPhoneNumber()));
    }

    // 보내는 참여자들의 분담액은 모두 같다.
    private Map<Long, Long> seenAmounts(Settlement settlement) {
        return Map.of(settlement.getId(), settlement.getShareOf(sender.getId()).getAmount());
    }

    private RecordSettlementCommand recordCommand(UUID clientRequestId) {
        return new RecordSettlementCommand(
                host.getId(),
                gathering.getId(),
                payer.getId(),
                clientRequestId,
                gathering.getRounds().getFirst().getId(),
                SettlementFixture.DINNER.getTotalAmount(),
                participantIds(),
                Map.of());
    }

    private List<Long> participantIds() {
        return gathering.getParticipants().stream().map(Participant::getId).toList();
    }

    private Long recordedId() {
        return settlementsOfGathering().getFirst().getId();
    }

    private List<Settlement> settlementsOfGathering() {
        return transactionTemplate.execute(
                _ -> settlementRepository.findAllByGatheringId(gathering.getId()));
    }

    // 인원은 그대로 두고 총액만 바꿔, 분담금 상태를 바꾸는 요청과 같은 분담금을 두고 겹치게 한다.
    private UpdateSettlementCommand updateCommand(Long settlementId, SettlementFixture fixture) {
        return new UpdateSettlementCommand(
                host.getId(),
                gathering.getId(),
                settlementId,
                gathering.getRounds().getFirst().getId(),
                fixture.getTotalAmount(),
                participantIds(),
                Map.of());
    }

    private Settlement findSettlement(Long settlementId) {
        return transactionTemplate.execute(
                _ -> settlementRepository.findById(settlementId).orElseThrow());
    }

    private List<Long> shareAmounts(Settlement settlement) {
        return settlement.getShares().stream().map(SettlementShare::getAmount).sorted().toList();
    }

    private List<Long> expectedShareAmounts(SettlementFixture fixture) {
        return fixture.createSplitMethod(payer.getId(), participantIds()).shares().stream()
                .map(SettlementShare::getAmount)
                .sorted()
                .toList();
    }

    private List<Long> sentShareParticipantIds(Settlement settlement) {
        return settlement.getShares().stream()
                .filter(share -> share.hasStatus(ShareStatus.SENT))
                .map(SettlementShare::getParticipantId)
                .toList();
    }
}
