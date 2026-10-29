package com.symphonia.gathering.fixture;

import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.entity.Participant;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GatheringFixture {
    FRIDAY_DINNER(1L, "금요일 회식");

    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 10, 27, 18, 0);

    private final Long hostMemberId;
    private final String name;

    public Gathering create() {
        return createWithParticipants(List.of());
    }

    public Gathering createWithParticipants(List<Participant> participants) {
        return Gathering.builder()
                .hostMemberId(hostMemberId)
                .name(name)
                .createdAt(CREATED_AT)
                .participants(participants)
                .build();
    }
}
