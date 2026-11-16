package com.symphonia.gathering.fixture;

import com.symphonia.gathering.domain.entity.Participant;
import com.symphonia.gathering.domain.policy.GatheringPolicy;
import com.symphonia.gathering.domain.vo.PhoneNumber;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ParticipantFixture {
    KIM("김철수", "01012345678"),
    LEE("이영희", "01098765432"),
    PARK("박민수", "01055556666");

    private static final LocalDateTime CONSENTED_AT = LocalDateTime.of(2026, 10, 27, 19, 0);

    private final String name;
    private final String phoneNumber;

    public Participant create() {
        return createWithPhoneNumber(phoneNumber);
    }

    public Participant createWithPhoneNumber(String phoneNumber) {
        return createWithIdAndPhoneNumber(null, phoneNumber);
    }

    public Participant createWithId(Long id) {
        return createWithIdAndPhoneNumber(id, phoneNumber);
    }

    // 주어진 id마다 참여자를 하나씩 만든다. 이름과 휴대폰은 KIM, LEE, PARK 순서로 돌려 쓰므로 id 외의 값은 서로 겹칠 수 있다.
    public static List<Participant> createWithIds(List<Long> ids) {
        ParticipantFixture[] fixtures = values();

        return IntStream.range(0, ids.size())
                .mapToObj(index -> fixtures[index % fixtures.length].createWithId(ids.get(index)))
                .toList();
    }

    private Participant createWithIdAndPhoneNumber(Long id, String phoneNumber) {
        return Participant.builder()
                .id(id)
                .name(name)
                .phoneNumber(new PhoneNumber(phoneNumber))
                .consentVersion(GatheringPolicy.CONSENT_VERSION)
                .consentedAt(CONSENTED_AT)
                .build();
    }
}
