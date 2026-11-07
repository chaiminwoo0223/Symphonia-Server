package com.symphonia.gathering.presentation.scheduler;

import com.symphonia.gathering.application.usecase.GetExpiredGatheringsUseCase;
import com.symphonia.gathering.application.usecase.PurgeGatheringUseCase;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 파기 시각이 지난 모임의 개인정보를 주기적으로 파기한다(#128).
// 지연 메시지는 유실되면 데이터가 영구히 남아 쓰지 않는다. 스윕은 조건만 보므로 한 회차를 놓쳐도 다음 회차에 파기한다.
// 여러 서버가 함께 돌아도 모임을 잠그고 다른 서버가 잠근 모임은 건너뛰므로 한 번만 파기한다.
// 테스트는 시각을 고정해 UseCase를 직접 부르므로, 테스트 프로파일에서는 이 스케줄러를 띄우지 않는다.
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "gathering.purge.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class GatheringPurgeScheduler {
    private final GetExpiredGatheringsUseCase getExpiredGatheringsUseCase;
    private final PurgeGatheringUseCase purgeGatheringUseCase;

    // 파기할 모임은 잠그지 않고 한 번에 골라, 아직 파기 시각이 남은 모임에는 트랜잭션을 열지 않는다.
    @Scheduled(fixedDelayString = "${gathering.purge.interval}")
    public void purgeExpiredGatherings() {
        getExpiredGatheringsUseCase.getExpiredGatheringIds().forEach(this::purge);
    }

    // 모임마다 트랜잭션을 나눠, 한 모임이 계속 실패해도 다른 모임은 보유 기간 안에 파기된다.
    // 실패한 모임은 다음 회차에 다시 시도한다. 로그에는 개인정보 없이 모임 id만 남긴다.
    private void purge(Long gatheringId) {
        try {
            Optional.of(gatheringId)
                    .filter(purgeGatheringUseCase::discardDataKeyIfExpired)
                    .ifPresent(purgeGatheringUseCase::purge);
        } catch (RuntimeException e) {
            log.error("[Gathering] gatheringId={} 파기 실패", gatheringId, e);
        }
    }
}
