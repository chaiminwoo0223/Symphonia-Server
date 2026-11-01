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

    private static final LocalDateTime OPENED_AT = LocalDateTime.of(2026, 10, 27, 18, 0);

    private final Long hostMemberId;
    private final String name;

    public Gathering create() {
        return createWithParticipants(List.of());
    }

    public Gathering createWithParticipants(List<Participant> participants) {
        return Gathering.builder()
                .hostMemberId(hostMemberId)
                .name(name)
                .openedAt(OPENED_AT)
                .participants(participants)
                .build();
    }

    public Gathering createWithHostMemberIdAndOpenedAt(Long hostMemberId, LocalDateTime openedAt) {
        return Gathering.open(hostMemberId, name, openedAt);
    }

    public Gathering createWithIdAndOpenedAt(Long id, LocalDateTime openedAt) {
        return createWithIdAndOpenedAt(id, openedAt, List.of());
    }

    public Gathering createWithIdAndOpenedAt(
            Long id, LocalDateTime openedAt, List<Participant> participants) {
        return Gathering.builder()
                .id(id)
                .hostMemberId(hostMemberId)
                .name(name)
                .openedAt(openedAt)
                .participants(participants)
                .build();
    }
}
