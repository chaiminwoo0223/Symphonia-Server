package com.symphonia.gathering.presentation.dto.request;

import com.symphonia.gathering.application.dto.command.OpenGatheringCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record OpenGatheringRequest(
        @NotNull
                @Schema(
                        description =
                                "열기 요청마다 클라이언트가 만드는 UUID. 같은 모임장이 같은 값으로 다시 보내면 모임을 새로 만들지 않고 처음 연 모임을 돌려주며, 참여 토큰은 새로 발급해 기존 토큰은 무효가 된다. 같은 값의 요청이 동시에 오면 먼저 처리한 요청만 성공하고 나머지는 409(GATHERING_OPEN_CONFLICT)이며, 이때는 같은 값으로 다시 보내면 된다")
                UUID clientRequestId,
        @NotBlank @Size(max = 50) @Schema(description = "모임 이름") String name,
        @Valid @Schema(description = "추천 결과에서 고른 페어링 (선택, 나중에 모임장이 고를 수 있음)")
                GatheringPairingRequest pairing) {
    public OpenGatheringCommand toCommand(String memberId) {
        return new OpenGatheringCommand(
                Long.parseLong(memberId),
                clientRequestId,
                name,
                pairing == null ? null : pairing.toCommand());
    }
}
