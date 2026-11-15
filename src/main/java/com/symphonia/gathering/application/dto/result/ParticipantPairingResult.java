package com.symphonia.gathering.application.dto.result;

import com.symphonia.catalog.application.dto.result.CatalogReferencesResult;

// 정산 페이지에서 피드백을 묻는 페어링이다. feedbackSubmitted가 true면 이미 낸 참여자라 묻지 않는다.
// anjuId는 정산 페이지가 이 안주를 곁들일 맛집을 조회할 때 쓴다.
public record ParticipantPairingResult(
        String drinkName, Long anjuId, String anjuName, boolean feedbackSubmitted) {
    public static ParticipantPairingResult of(
            Long anjuId, CatalogReferencesResult catalogReferences, boolean feedbackSubmitted) {
        return new ParticipantPairingResult(
                catalogReferences.drinkName(),
                anjuId,
                catalogReferences.anjuName(),
                feedbackSubmitted);
    }
}
