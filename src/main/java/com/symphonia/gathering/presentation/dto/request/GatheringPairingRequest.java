package com.symphonia.gathering.presentation.dto.request;

import com.symphonia.gathering.application.dto.command.ChooseGatheringPairingCommand;
import com.symphonia.gathering.application.dto.command.GatheringPairingCommand;
import com.symphonia.pairing.domain.vo.MoodType;
import com.symphonia.pairing.domain.vo.RelationshipType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record GatheringPairingRequest(
        @NotNull @Schema(description = "술 ID") Long drinkId,
        @NotNull @Schema(description = "안주 ID") Long anjuId,
        @NotNull @Schema(description = "추천받을 때 고른 관계 유형") RelationshipType relationshipType,
        @Schema(description = "추천받을 때 고른 분위기 유형(선택)") MoodType moodType) {
    public ChooseGatheringPairingCommand toChooseCommand(String memberId, Long gatheringId) {
        return ChooseGatheringPairingCommand.of(Long.parseLong(memberId), gatheringId, toCommand());
    }

    public GatheringPairingCommand toCommand() {
        return new GatheringPairingCommand(drinkId, anjuId, relationshipType, moodType);
    }
}
