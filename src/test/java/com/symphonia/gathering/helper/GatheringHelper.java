package com.symphonia.gathering.helper;

import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.entity.Participant;
import com.symphonia.gathering.domain.policy.GatheringPolicy;
import com.symphonia.gathering.domain.provider.GatheringTokenProvider;
import com.symphonia.gathering.domain.repository.GatheringJoinTokenRepository;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.domain.repository.ParticipantLinkTokenRepository;
import com.symphonia.gathering.domain.vo.PhoneNumber;
import com.symphonia.gathering.fixture.GatheringFixture;
import com.symphonia.gathering.fixture.ParticipantFixture;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GatheringHelper {

    private final GatheringRepository gatheringRepository;
    private final GatheringJoinTokenRepository gatheringJoinTokenRepository;
    private final ParticipantLinkTokenRepository participantLinkTokenRepository;
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

    // 참여 흐름처럼 참여자를 더해 저장하고, id가 채워진 참여자를 담은 모임을 돌려준다.
    public Gathering join(Gathering gathering, ParticipantFixture... participantFixtures) {
        for (ParticipantFixture participantFixture : participantFixtures) {
            gathering.join(
                    participantFixture.getName(),
                    new PhoneNumber(participantFixture.getPhoneNumber()),
                    GatheringPolicy.CONSENT_VERSION,
                    LocalDateTime.now());
        }

        return gatheringRepository.save(gathering);
    }

    // 저장소에는 해시만 남으므로, 요청에 쓸 원본 개인 링크 토큰을 돌려준다.
    public String issueLinkToken(Participant participant) {
        String linkToken = gatheringTokenProvider.generate();
        participantLinkTokenRepository.save(linkToken, participant.getId());

        return linkToken;
    }
}
