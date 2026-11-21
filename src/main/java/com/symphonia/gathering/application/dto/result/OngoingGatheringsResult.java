package com.symphonia.gathering.application.dto.result;

import com.symphonia.gathering.domain.vo.GatheringRetention;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

// 회원이 모임장이고 파기 시각이 지나지 않은 모임이다. 다른 도메인이 탈퇴 전에 경고할 때 쓴다.
// lastDeletesAt은 그 모임들의 개인정보를 파기할 시각 중 가장 늦은 시각이고, 진행 중인 모임이 없으면 null이다.
public record OngoingGatheringsResult(int count, LocalDateTime lastDeletesAt) {
    public static OngoingGatheringsResult from(List<GatheringRetention> ongoingRetentions) {
        return new OngoingGatheringsResult(
                ongoingRetentions.size(),
                ongoingRetentions.stream()
                        .map(GatheringRetention::deletesAt)
                        .max(Comparator.naturalOrder())
                        .orElse(null));
    }
}
