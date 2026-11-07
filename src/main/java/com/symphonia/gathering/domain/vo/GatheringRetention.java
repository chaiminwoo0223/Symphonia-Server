package com.symphonia.gathering.domain.vo;

import com.symphonia.gathering.domain.entity.Settlement;
import com.symphonia.gathering.domain.policy.GatheringPolicy;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.BinaryOperator;

// 모임의 개인정보를 언제 파기하는지 정한다(#128).
// 모든 분담금이 끝나고 7일 뒤에 파기하고, 끝나지 않았어도 모임을 연 지 29일 뒤에는 파기한다.
// 참여 화면이 안내하는 보유 기간은 최대 30일이다. 1시간 주기 스윕이 늦거나 몇 회차 실패해도 30일 안에 파기하도록 하루를 남긴다.
// 데이터 키를 지운 모임은 개인정보를 복호화할 수 없으므로 파기 시각과 관계없이 파기가 확정된 것으로 본다.
public record GatheringRetention(
        Long gatheringId, LocalDateTime deletesAt, boolean dataKeyDiscarded) {
    private static final Duration MAX_RETENTION = Duration.ofDays(29);
    private static final Duration RETENTION_AFTER_COMPLETION = Duration.ofDays(7);

    // settlements는 취소한 정산 건까지 모임의 정산 건을 모두 담는다.
    public static GatheringRetention of(
            Long gatheringId,
            LocalDateTime openedAt,
            List<Settlement> settlements,
            boolean dataKeyDiscarded) {
        LocalDateTime maxDeletesAt = openedAt.plus(MAX_RETENTION);
        LocalDateTime deletesAt =
                completedAt(openedAt, settlements)
                        .map(completedAt -> completedAt.plus(RETENTION_AFTER_COMPLETION))
                        .filter(
                                deletesAtAfterCompletion ->
                                        deletesAtAfterCompletion.isBefore(maxDeletesAt))
                        .orElse(maxDeletesAt);

        return new GatheringRetention(gatheringId, deletesAt, dataKeyDiscarded);
    }

    // 정산은 빨라도 기록 기간이 끝나야 끝나므로, 가장 빠른 파기 시각은 기록 기간이 끝나고 7일 뒤다.
    // 이 시각보다 늦게 연 모임은 아직 파기 대상이 될 수 없어, 파기 스윕은 이 시각까지 연 모임만 후보로 읽는다.
    public static LocalDateTime latestExpirableOpenedAt(LocalDateTime now) {
        return now.minus(RETENTION_AFTER_COMPLETION).minus(GatheringPolicy.RECORDING_PERIOD);
    }

    public boolean isExpired(LocalDateTime now) {
        return dataKeyDiscarded || !now.isBefore(deletesAt);
    }

    // 정산이 끝나지 않았으면 끝난 시각이 없어 빈 값을 돌려준다.
    private static Optional<LocalDateTime> completedAt(
            LocalDateTime openedAt, List<Settlement> settlements) {
        return Optional.of(settlements)
                .filter(GatheringRetention::isSettled)
                .map(settled -> lastChangedAt(openedAt, settled));
    }

    // 취소하지 않은 정산 건에 미입금 분담금이 남았으면 정산이 끝나지 않았다.
    private static boolean isSettled(List<Settlement> settlements) {
        return settlements.stream().noneMatch(Settlement::hasUnpaidShares);
    }

    // 기록 기간이 끝난 시각에서 시작해, 분담금 상태를 바꾼 시각과 정산 건을 취소한 시각 중 더 늦은 시각으로 바꿔 간다.
    // 기록 기간 안에는 정산 건이 더 들어올 수 있고, 정산 건이 없는 모임도 기록 기간이 끝나면 더 쓸 일이 없기 때문이다.
    private static LocalDateTime lastChangedAt(
            LocalDateTime openedAt, List<Settlement> settlements) {
        return settlements.stream()
                .map(Settlement::getLastChangedAt)
                .flatMap(Optional::stream)
                .reduce(
                        GatheringPolicy.recordingClosesAt(openedAt),
                        BinaryOperator.maxBy(Comparator.naturalOrder()));
    }
}
