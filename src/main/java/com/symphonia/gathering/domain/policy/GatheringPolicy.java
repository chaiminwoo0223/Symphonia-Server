package com.symphonia.gathering.domain.policy;

import com.symphonia.gathering.domain.exception.ConsentRequiredException;
import com.symphonia.gathering.domain.exception.GatheringJoinClosedException;
import com.symphonia.gathering.domain.exception.NotGatheringHostException;
import com.symphonia.gathering.domain.exception.OutdatedConsentVersionException;
import com.symphonia.gathering.domain.exception.ParticipantAlreadyJoinedException;
import java.time.Duration;
import java.time.LocalDateTime;

public enum GatheringPolicy {
    ;

    // 참여 화면이 안내하는 수집 항목, 목적, 보유 기간 문구의 버전이다. 문구를 바꾸면 이 값도 바꾼다.
    public static final String CONSENT_VERSION = "2026-10-30";

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

    // 바뀌기 전 안내를 띄운 화면에서 받은 동의는 무엇에 동의했는지 입증할 수 없어 거절한다.
    public static void validateConsent(boolean consentAgreed, String consentVersion) {
        if (!consentAgreed) {
            throw new ConsentRequiredException();
        }
        if (!CONSENT_VERSION.equals(consentVersion)) {
            throw new OutdatedConsentVersionException();
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
