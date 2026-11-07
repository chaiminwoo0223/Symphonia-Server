package com.symphonia.gathering.application.dto.result;

import java.time.LocalDateTime;

// deletesAt은 모임의 개인정보를 파기할 시각이다. 모든 분담금이 끝나면 앞당겨지고, 미입금으로 되돌리면 다시 늦춰진다.
public record GatheringResult(
        String name, boolean joinable, String consentVersion, LocalDateTime deletesAt) {
    public static GatheringResult of(
            String name, boolean joinable, String consentVersion, LocalDateTime deletesAt) {
        return new GatheringResult(name, joinable, consentVersion, deletesAt);
    }
}
