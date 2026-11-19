package com.symphonia.gathering.application.service;

import com.symphonia.common.annotation.CommandService;
import com.symphonia.gathering.application.dto.command.AddRoundCommand;
import com.symphonia.gathering.application.dto.result.GatheringRoundResult;
import com.symphonia.gathering.application.usecase.AddRoundUseCase;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.exception.GatheringNotFoundException;
import com.symphonia.gathering.domain.policy.GatheringPolicy;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.domain.repository.GatheringRetentionRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;

// 모임을 만든 회원만 차수를 추가한다. 참여자가 추가하면 모임장이 없던 자리의 정산 건에 모임장이 들어갈 수 있기 때문이다.
@CommandService
@RequiredArgsConstructor
public class RoundCommandService implements AddRoundUseCase {
    private final GatheringRepository gatheringRepository;
    private final GatheringRetentionRepository gatheringRetentionRepository;
    private final Clock clock;

    // 요청한 번호의 차수가 이미 있으면 새로 만들지 않고 돌려준다. 같은 화면을 두 번 누르거나 두 요청이 동시에 와도 차수가 둘 생기지 않게 하기 위해서다.
    @Override
    public GatheringRoundResult add(AddRoundCommand command) {
        LocalDateTime now = LocalDateTime.now(clock);
        validateRetained(command.gatheringId(), now);
        Gathering gathering =
                gatheringRepository
                        .findById(command.gatheringId())
                        .orElseThrow(GatheringNotFoundException::new);
        GatheringPolicy.validateHost(gathering.getHostMemberId(), command.memberId());
        GatheringPolicy.validateRoundAddable(gathering.getOpenedAt(), now);

        return gathering
                .findRound(command.roundNumber())
                .map(GatheringRoundResult::from)
                .orElseGet(() -> addRound(gathering, command.roundNumber()));
    }

    // 동시에 같은 번호를 추가했으면 저장은 먼저 넣은 쪽의 행을 남기므로, 저장 뒤 읽은 차수를 돌려준다.
    private GatheringRoundResult addRound(Gathering gathering, int roundNumber) {
        GatheringPolicy.validateRoundNumber(roundNumber, gathering.getLastRoundNumber());

        gathering.addRound(roundNumber);
        Gathering savedGathering = gatheringRepository.save(gathering);

        return savedGathering.findRound(roundNumber).map(GatheringRoundResult::from).orElseThrow();
    }

    // 파기 시각이 지난 모임은 물리 삭제 전이라도 찾지 못한 것으로 본다. 삭제 뒤의 응답과 같게 하기 위해서다.
    private void validateRetained(Long gatheringId, LocalDateTime now) {
        gatheringRetentionRepository
                .findByGatheringId(gatheringId)
                .filter(retention -> !retention.isExpired(now))
                .orElseThrow(GatheringNotFoundException::new);
    }
}
