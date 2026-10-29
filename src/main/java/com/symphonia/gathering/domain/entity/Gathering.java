package com.symphonia.gathering.domain.entity;

import com.symphonia.gathering.domain.vo.PhoneNumber;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Gathering {
    private Long id;
    private Long hostMemberId;
    private String name;
    private LocalDateTime createdAt;
    private List<Participant> participants;

    public static Gathering of(Long hostMemberId, String name) {
        return Gathering.builder()
                .hostMemberId(hostMemberId)
                .name(name)
                .participants(List.of())
                .build();
    }

    // 참여자 목록을 바깥에서 바꿀 수 없게, 참여할 때마다 기존 목록에 더한 새 불변 목록으로 바꾼다.
    public void join(String name, PhoneNumber phoneNumber, boolean nonDrinker, LocalDateTime now) {
        Participant participant = Participant.of(name, phoneNumber, nonDrinker, now);
        participants = Stream.concat(participants.stream(), Stream.of(participant)).toList();
    }

    public List<Participant> getParticipants() {
        return List.copyOf(participants);
    }
}
