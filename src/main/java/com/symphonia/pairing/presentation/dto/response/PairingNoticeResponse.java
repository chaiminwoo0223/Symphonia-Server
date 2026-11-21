package com.symphonia.pairing.presentation.dto.response;

import com.symphonia.pairing.application.dto.result.PairingNoticeResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record PairingNoticeResponse(
        @Schema(
                        description =
                                "안내 코드. NO_DRINK_WITHIN_ABV_LIMIT, NO_ANJU_WITHOUT_ALLERGEN, NO_NON_ALCOHOLIC_DRINK 중 하나입니다.")
                String code,
        @Schema(description = "안내 문구") String message) {
    public static PairingNoticeResponse from(PairingNoticeResult result) {
        return new PairingNoticeResponse(result.code(), result.message());
    }
}
