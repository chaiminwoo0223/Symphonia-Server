package com.symphonia.gathering.application.dto.result;

import com.symphonia.catalog.application.dto.result.CatalogReferencesResult;
import com.symphonia.gathering.domain.vo.GatheringPairing;

// 정산 페이지에서 피드백을 묻는 페어링이다. feedbackSubmitted가 true면 이미 낸 참여자라 묻지 않는다.
// feedbackSubmittable이 false면 아직 낼 수 없거나 이미 낸 참여자라 피드백 카드를 띄우지 않는다.
// drinkId와 anjuId는 피드백을 낼 때 참여자가 본 조합으로 되돌려 보낸다. anjuId는 정산 페이지가 이 안주를 곁들일 맛집을 조회할 때도 쓴다.
public record ParticipantPairingResult(
        Long drinkId,
        String drinkName,
        Long anjuId,
        String anjuName,
        boolean feedbackSubmitted,
        boolean feedbackSubmittable) {
    public static ParticipantPairingResult of(
            GatheringPairing gatheringPairing,
            CatalogReferencesResult catalogReferences,
            boolean feedbackSubmitted,
            boolean feedbackSubmittable) {
        return new ParticipantPairingResult(
                gatheringPairing.drinkId(),
                catalogReferences.drinkName(),
                gatheringPairing.anjuId(),
                catalogReferences.anjuName(),
                feedbackSubmitted,
                feedbackSubmittable);
    }
}
