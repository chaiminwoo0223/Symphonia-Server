package com.symphonia.gathering.application.dto.result;

import com.symphonia.catalog.application.dto.result.CatalogReferencesResult;

// 정산 페이지에서 피드백을 묻는 페어링이다. feedbackSubmitted가 true면 이미 낸 참여자라 묻지 않는다.
public record ParticipantPairingResult(
        String drinkName,
        String anjuName,
        String musicMoodTitle,
        String streamingUrl,
        boolean feedbackSubmitted) {
    public static ParticipantPairingResult of(
            CatalogReferencesResult catalogReferences, boolean feedbackSubmitted) {
        return new ParticipantPairingResult(
                catalogReferences.drinkName(),
                catalogReferences.anjuName(),
                catalogReferences.musicMoodTitle(),
                catalogReferences.streamingUrl(),
                feedbackSubmitted);
    }
}
