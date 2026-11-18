package com.symphonia.gathering.application.dto.result;

import com.symphonia.gathering.domain.vo.JoinClosedReason;
import java.time.LocalDateTime;

// deletesAt은 모임의 개인정보를 파기할 시각이다. 모든 분담금이 끝나면 앞당겨지고, 미입금으로 되돌리면 다시 늦춰진다.
// joinClosedReason은 참여할 수 있으면 null이다. 클라이언트가 사유별로 다른 안내를 보여 줄 수 있게 한다.
public record GatheringResult(
        String name,
        boolean joinable,
        JoinClosedReason joinClosedReason,
        String consentVersion,
        LocalDateTime deletesAt) {
    public static GatheringResult of(
            String name,
            JoinClosedReason joinClosedReason,
            String consentVersion,
            LocalDateTime deletesAt) {
        return new GatheringResult(
                name, joinClosedReason == null, joinClosedReason, consentVersion, deletesAt);
    }
}
