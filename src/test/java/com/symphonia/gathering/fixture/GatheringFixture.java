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

    // 생성 시각은 저장할 때 채워지므로, 실제 생성 흐름처럼 생성 시각 없이 만든다.
    public Gathering createWithHostMemberId(Long hostMemberId) {
        return Gathering.of(hostMemberId, name);
    }

    public Gathering createWithIdAndCreatedAt(Long id, LocalDateTime createdAt) {
        return createWithIdAndCreatedAt(id, createdAt, List.of());
    }

    public Gathering createWithIdAndCreatedAt(
            Long id, LocalDateTime createdAt, List<Participant> participants) {
        return Gathering.builder()
                .id(id)
                .hostMemberId(hostMemberId)
                .name(name)
                .createdAt(createdAt)
                .participants(participants)
                .build();
    }
}
