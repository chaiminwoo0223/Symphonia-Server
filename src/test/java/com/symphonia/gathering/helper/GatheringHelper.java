package com.symphonia.gathering.helper;

import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.provider.GatheringTokenProvider;
import com.symphonia.gathering.domain.repository.GatheringJoinTokenRepository;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.fixture.GatheringFixture;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GatheringHelper {

    private final GatheringRepository gatheringRepository;
    private final GatheringJoinTokenRepository gatheringJoinTokenRepository;
    private final GatheringTokenProvider gatheringTokenProvider;

    public Gathering save(GatheringFixture gatheringFixture, Long hostMemberId) {
        return gatheringRepository.save(gatheringFixture.createWithHostMemberId(hostMemberId));
    }

    // 저장소에는 해시만 남으므로, 요청에 쓸 원본 참여 토큰을 돌려준다.
    public String issueJoinToken(Gathering gathering) {
        String joinToken = gatheringTokenProvider.generate();
        gatheringJoinTokenRepository.save(joinToken, gathering.getId());

        return joinToken;
    }
}
