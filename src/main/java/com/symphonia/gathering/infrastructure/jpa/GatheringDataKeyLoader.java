package com.symphonia.gathering.infrastructure.jpa;

import com.symphonia.gathering.domain.exception.GatheringDataKeyNotFoundException;
import com.symphonia.gathering.infrastructure.crypto.GatheringDataKey;
import com.symphonia.gathering.infrastructure.crypto.GatheringMasterKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// 모임의 개인정보를 암호화하는 저장소들이 같은 방식으로 모임별 데이터 키를 풀어 쓰게 한다.
@Component
@RequiredArgsConstructor
public class GatheringDataKeyLoader {
    private final GatheringDataKeyJpaRepository gatheringDataKeyJpaRepository;
    private final GatheringMasterKey gatheringMasterKey;

    public GatheringDataKey load(Long gatheringId) {
        GatheringDataKeyJpaEntity dataKeyEntity =
                gatheringDataKeyJpaRepository
                        .findById(gatheringId)
                        .orElseThrow(GatheringDataKeyNotFoundException::new);

        return gatheringMasterKey.unwrap(dataKeyEntity.toWrappedDataKey());
    }
}
