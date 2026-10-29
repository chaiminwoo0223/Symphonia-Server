package com.symphonia.gathering.domain.policy;

import com.symphonia.gathering.domain.exception.GatheringJoinClosedException;
import com.symphonia.gathering.domain.exception.NotGatheringHostException;
import com.symphonia.gathering.domain.exception.ParticipantAlreadyJoinedException;
import java.time.Duration;
import java.time.LocalDateTime;

public enum GatheringPolicy {
    ;

    // 모임 생성 후 정산 건을 기록할 수 있는 기간이다. 이 기간이 지나 참여한 사람은 분담에 들어갈 수 없어 참여도 함께 막는다.
    private static final Duration RECORDING_PERIOD = Duration.ofHours(48);

    public static boolean isWithinRecordingPeriod(LocalDateTime createdAt, LocalDateTime now) {
        return now.isBefore(createdAt.plus(RECORDING_PERIOD));
    }

    public static void validateJoinable(LocalDateTime createdAt, LocalDateTime now) {
        if (!isWithinRecordingPeriod(createdAt, now)) {
            throw new GatheringJoinClosedException();
        }
    }

    public static void validateNotJoined(boolean alreadyJoined) {
        if (alreadyJoined) {
            throw new ParticipantAlreadyJoinedException();
        }
    }

    public static void validateHost(Long hostMemberId, Long memberId) {
        if (!hostMemberId.equals(memberId)) {
            throw new NotGatheringHostException();
        }
    }
}
