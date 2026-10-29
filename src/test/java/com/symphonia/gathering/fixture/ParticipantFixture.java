package com.symphonia.gathering.fixture;

import com.symphonia.gathering.domain.entity.Participant;
import com.symphonia.gathering.domain.policy.GatheringPolicy;
import com.symphonia.gathering.domain.vo.PhoneNumber;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ParticipantFixture {
    KIM("김철수", "01012345678", false),
    LEE("이영희", "01098765432", true);

    private static final LocalDateTime CONSENTED_AT = LocalDateTime.of(2026, 10, 27, 19, 0);

    private final String name;
    private final String phoneNumber;
    private final boolean nonDrinker;

    public Participant create() {
        return createWithPhoneNumber(phoneNumber);
    }

    public Participant createWithPhoneNumber(String phoneNumber) {
        return Participant.builder()
                .name(name)
                .phoneNumber(new PhoneNumber(phoneNumber))
                .nonDrinker(nonDrinker)
                .consentVersion(GatheringPolicy.CONSENT_VERSION)
                .consentedAt(CONSENTED_AT)
                .build();
    }
}
