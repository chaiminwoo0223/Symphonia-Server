package com.symphonia.gathering.domain.policy;

import com.symphonia.gathering.domain.entity.Settlement;
import com.symphonia.gathering.domain.exception.GatheringRoundNotFoundException;
import com.symphonia.gathering.domain.exception.InvalidSettlementParticipantsException;
import com.symphonia.gathering.domain.exception.NotSettlementPayerException;
import com.symphonia.gathering.domain.exception.SentShareRequiredException;
import com.symphonia.gathering.domain.exception.SettlementRecordingClosedException;
import com.symphonia.gathering.domain.exception.SettlementShareNotFoundException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// 정산 건과 모임을 함께 봐야 하는 검증과 계산이다. 검증은 Service가 Settlement의 상태를 바꾸기 전에 호출한다.
public enum SettlementPolicy {
    ;

    public static void validateRecordable(LocalDateTime gatheringOpenedAt, LocalDateTime now) {
        if (!GatheringPolicy.isWithinRecordingPeriod(gatheringOpenedAt, now)) {
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

    public static void validateSharesFound(boolean found) {
        if (!found) {
            throw new SettlementShareNotFoundException();
        }
    }

    // 차수의 참여자는 그 차수에 속한 취소하지 않은 정산 건 인원의 합집합이다. 정산 건이 없거나 모두 취소한 차수는 키가 없다.
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

    // 정산에서 돈이 오간 참여자만 페어링 피드백을 낼 수 있다. 결제자로 지정된 정산 건과 분담금이 있는 정산 건을 함께 본다.
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
}
