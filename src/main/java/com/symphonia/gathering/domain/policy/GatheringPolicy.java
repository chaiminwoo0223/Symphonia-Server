package com.symphonia.gathering.domain.policy;

import com.symphonia.gathering.domain.exception.ConsentRequiredException;
import com.symphonia.gathering.domain.exception.GatheringJoinClosedException;
import com.symphonia.gathering.domain.exception.GatheringPairingClosedException;
import com.symphonia.gathering.domain.exception.GatheringRoundClosedException;
import com.symphonia.gathering.domain.exception.GatheringRoundCountExceedsLimitException;
import com.symphonia.gathering.domain.exception.InvalidGatheringRoundNumberException;
import com.symphonia.gathering.domain.exception.NotGatheringHostException;
import com.symphonia.gathering.domain.exception.OutdatedConsentVersionException;
import com.symphonia.gathering.domain.exception.PairingFeedbackAlreadySubmittedException;
import com.symphonia.gathering.domain.exception.ParticipantAlreadyJoinedException;
import java.time.Duration;
import java.time.LocalDateTime;

public enum GatheringPolicy {
    ;

    // 참여 화면이 안내하는 수집 항목, 목적, 보유 기간 문구의 버전이다. 문구를 바꾸면 이 값도 바꾼다.
    public static final String CONSENT_VERSION = "2026-10-30";

    // 모임 생성 후 정산 건을 기록할 수 있는 기간이다. 이 기간이 지나 참여한 사람은 분담에 들어갈 수 없어 참여도 함께 막는다.
    public static final Duration RECORDING_PERIOD = Duration.ofHours(48);

    // 참여자 누구나 차수를 추가할 수 있어서, 빈 차수가 무제한 쌓이지 않도록 한 모임의 차수 수를 제한한다.
    public static final int MAX_ROUND_COUNT = 10;

    public static boolean isWithinRecordingPeriod(LocalDateTime openedAt, LocalDateTime now) {
        return now.isBefore(recordingClosesAt(openedAt));
    }

    public static LocalDateTime recordingClosesAt(LocalDateTime openedAt) {
        return openedAt.plus(RECORDING_PERIOD);
    }

    public static void validateJoinable(LocalDateTime openedAt, LocalDateTime now) {
        if (!isWithinRecordingPeriod(openedAt, now)) {
            throw new GatheringJoinClosedException();
        }
    }

    // 정산 건을 기록할 수 없는 기간에는 차수를 늘릴 이유가 없다.
    public static void validateRoundAddable(LocalDateTime openedAt, LocalDateTime now) {
        if (!isWithinRecordingPeriod(openedAt, now)) {
            throw new GatheringRoundClosedException();
        }
    }

    // 상한을 넘는 번호는 얼마나 건너뛰었든 상한 초과가 원인이라 먼저 거절한다. 상한 안에서는 마지막 차수의 다음 번호까지만
    // 추가할 수 있어 번호가 빈틈없이 이어진다.
    public static void validateRoundNumber(int requestedNumber, int lastRoundNumber) {
        if (requestedNumber > MAX_ROUND_COUNT) {
            throw new GatheringRoundCountExceedsLimitException();
        }
        if (requestedNumber > lastRoundNumber + 1) {
            throw new InvalidGatheringRoundNumberException();
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

    // 모임장은 기록 기간 안에만 페어링을 고르거나 바꾼다. 실제로 먹은 조합을 바로잡는 기능이라 술자리가 끝나면 쓸 일이 없다.
    // 또 참여자가 피드백을 내는 동안 묻는 조합이 바뀌는 기간을 줄이기 위해서다.
    public static void validatePairingChangeable(LocalDateTime openedAt, LocalDateTime now) {
        if (!isWithinRecordingPeriod(openedAt, now)) {
            throw new GatheringPairingClosedException();
        }
    }

    public static void validatePairingFeedbackNotSubmitted(boolean submitted) {
        if (submitted) {
            throw new PairingFeedbackAlreadySubmittedException();
        }
    }
}
