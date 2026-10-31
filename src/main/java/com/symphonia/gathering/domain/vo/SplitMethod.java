package com.symphonia.gathering.domain.vo;

import com.symphonia.gathering.domain.entity.SettlementShare;
import com.symphonia.gathering.domain.exception.FixedAmountExceedsTotalException;
import com.symphonia.gathering.domain.exception.InvalidSettlementAmountException;
import com.symphonia.gathering.domain.exception.InvalidSettlementParticipantsException;
import com.symphonia.gathering.domain.exception.NotEnoughSettlementParticipantsException;
import com.symphonia.gathering.domain.policy.ShareRoundingPolicy;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// 분담 방식은 똑같이 나누기 하나다. 결제자가 금액을 지정한 참여자를 빼고, 나머지 참여자가 남은 금액을 똑같이 나눈다.
// 결제자는 항상 인원에 들어가고, 결제자 분담액은 총액에서 다른 참여자의 분담액 합을 뺀 값이라 지정할 수 없다.
public record SplitMethod(
        long totalAmount,
        Long payerParticipantId,
        List<Long> participantIds,
        Map<Long, Long> fixedAmounts) {
    private static final int MIN_PARTICIPANT_COUNT = 2;

    public SplitMethod {
        participantIds = List.copyOf(participantIds);
        fixedAmounts = Map.copyOf(fixedAmounts);
        validateAmounts(totalAmount, fixedAmounts);
        validateParticipants(payerParticipantId, participantIds, fixedAmounts);
        validateFixedAmountsWithinTotal(totalAmount, fixedAmounts);
    }

    public static SplitMethod of(
            long totalAmount,
            Long payerParticipantId,
            List<Long> participantIds,
            Map<Long, Long> fixedAmounts) {
        return new SplitMethod(totalAmount, payerParticipantId, participantIds, fixedAmounts);
    }

    // 결제자를 뺀 참여자의 분담을 인원 순서대로 돌려준다.
    // 금액을 지정하지 않은 참여자는 결제자와 함께 남은 금액을 똑같이 나눈다.
    public List<SettlementShare> shares() {
        long remainingAmount = totalAmount - fixedAmountSum(fixedAmounts);
        int equalSharerCount = participantIds.size() - fixedAmounts.size();
        long equalShare = ShareRoundingPolicy.equalShare(remainingAmount, equalSharerCount);

        return participantIds.stream()
                .filter(participantId -> !participantId.equals(payerParticipantId))
                .map(participantId -> toShare(participantId, equalShare))
                .toList();
    }

    private SettlementShare toShare(Long participantId, long equalShare) {
        return fixedAmounts.containsKey(participantId)
                ? SettlementShare.fixed(participantId, fixedAmounts.get(participantId))
                : SettlementShare.equal(participantId, equalShare);
    }

    private static void validateAmounts(long totalAmount, Map<Long, Long> fixedAmounts) {
        boolean negativeFixedAmount = fixedAmounts.values().stream().anyMatch(amount -> amount < 0);

        if (totalAmount <= 0 || negativeFixedAmount) {
            throw new InvalidSettlementAmountException();
        }
    }

    // 인원은 중복 없이 결제자를 포함한 두 명 이상이고, 금액은 결제자가 아닌 인원에게만 지정할 수 있다.
    private static void validateParticipants(
            Long payerParticipantId, List<Long> participantIds, Map<Long, Long> fixedAmounts) {
        Set<Long> uniqueParticipantIds = new HashSet<>(participantIds);
        boolean distinct = uniqueParticipantIds.size() == participantIds.size();
        boolean enough = participantIds.size() >= MIN_PARTICIPANT_COUNT;
        boolean includesPayer = uniqueParticipantIds.contains(payerParticipantId);
        boolean fixedForOthersOnly =
                uniqueParticipantIds.containsAll(fixedAmounts.keySet())
                        && !fixedAmounts.containsKey(payerParticipantId);

        if (!distinct || !includesPayer || !fixedForOthersOnly) {
            throw new InvalidSettlementParticipantsException();
        }
        if (!enough) {
            throw new NotEnoughSettlementParticipantsException();
        }
    }

    private static void validateFixedAmountsWithinTotal(
            long totalAmount, Map<Long, Long> fixedAmounts) {
        if (fixedAmountSum(fixedAmounts) > totalAmount) {
            throw new FixedAmountExceedsTotalException();
        }
    }

    // 지정 금액은 0 이상이라 합이 long 범위를 넘으면 총액보다 크다. 넘친 값이 음수로 돌아와 검사를 통과하지 않게 막는다.
    private static long fixedAmountSum(Map<Long, Long> fixedAmounts) {
        try {
            return fixedAmounts.values().stream().reduce(0L, Math::addExact);
        } catch (ArithmeticException e) {
            throw new FixedAmountExceedsTotalException();
        }
    }
}
