package com.symphonia.gathering.domain.policy;

import com.symphonia.gathering.domain.entity.Settlement;
import com.symphonia.gathering.domain.exception.GatheringRoundNotFoundException;
import com.symphonia.gathering.domain.exception.InvalidSettlementParticipantsException;
import com.symphonia.gathering.domain.exception.NotSettlementPayerException;
import com.symphonia.gathering.domain.exception.SentShareRequiredException;
import com.symphonia.gathering.domain.exception.SettlementRecordingClosedException;
import com.symphonia.gathering.domain.exception.UnpaidShareConflictException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// 정산 건과 모임을 함께 봐야 하는 검증과 계산이다. 검증은 Service가 Settlement의 상태를 바꾸기 전에 호출한다.
public enum SettlementPolicy {
    ;

    public static void validateRecordable(
            LocalDateTime gatheringRecordingClosesAt, LocalDateTime now) {
        if (!GatheringPolicy.isWithinRecordingPeriod(gatheringRecordingClosesAt, now)) {
            throw new SettlementRecordingClosedException();
        }
    }

    public static void validateGatheringParticipants(boolean allJoined) {
        if (!allJoined) {
            throw new InvalidSettlementParticipantsException();
        }
    }

    public static void validateGatheringRound(boolean found) {
        if (!found) {
            throw new GatheringRoundNotFoundException();
        }
    }

    public static void validateSeenShares(
            List<Settlement> settlements, Long participantId, Map<Long, Long> seenAmounts) {
        Optional.of(settlements)
                .filter(current -> containsSeenShares(current, participantId, seenAmounts))
                .filter(
                        current ->
                                containsAllUnpaidShares(
                                        current, participantId, seenAmounts.keySet()))
                .orElseThrow(UnpaidShareConflictException::new);
    }

    public static Map<Long, Set<Long>> getParticipantIdsByRoundId(List<Settlement> settlements) {
        return settlements.stream()
                .filter(settlement -> !settlement.isCancelled())
                .collect(
                        Collectors.groupingBy(
                                Settlement::getRoundId,
                                Collectors.flatMapping(
                                        settlement -> settlement.getParticipantIds().stream(),
                                        Collectors.toCollection(HashSet::new))));
    }

    public static boolean isPairingFeedbackSubmittable(
            Long participantId,
            List<Settlement> payerSettlements,
            List<Settlement> shareSettlements) {
        return Stream.concat(payerSettlements.stream(), shareSettlements.stream())
                .anyMatch(settlement -> settlement.isSettledFor(participantId));
    }

    public static void validatePairingFeedbackSubmittable(boolean submittable) {
        if (!submittable) {
            throw new SentShareRequiredException();
        }
    }

    public static void validatePayer(Long payerParticipantId, Long participantId) {
        if (!payerParticipantId.equals(participantId)) {
            throw new NotSettlementPayerException();
        }
    }

    // 본 정산 건이 모두 지금도 있고 본인 분담금이 본 금액과 같은지다.
    private static boolean containsSeenShares(
            List<Settlement> settlements, Long participantId, Map<Long, Long> seenAmounts) {
        Map<Long, Long> currentAmounts =
                settlements.stream()
                        .collect(
                                Collectors.toMap(
                                        Settlement::getId,
                                        settlement ->
                                                settlement.getShareOf(participantId).getAmount()));

        return currentAmounts.entrySet().containsAll(seenAmounts.entrySet());
    }

    // 지금 미입금인 분담금의 정산 건이 모두 본 정산 건에 있는지다.
    private static boolean containsAllUnpaidShares(
            List<Settlement> settlements, Long participantId, Set<Long> seenSettlementIds) {
        return settlements.stream()
                .filter(settlement -> settlement.hasUnpaidShareOf(participantId))
                .allMatch(settlement -> seenSettlementIds.contains(settlement.getId()));
    }
}
