package com.symphonia.gathering.domain.entity;

import com.symphonia.gathering.domain.exception.ParticipantNotFoundException;
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
    public void join(
            String name,
            PhoneNumber phoneNumber,
            boolean nonDrinker,
            String consentVersion,
            LocalDateTime now) {
        Participant participant =
                Participant.of(name, phoneNumber, nonDrinker, consentVersion, now);
        participants = Stream.concat(participants.stream(), Stream.of(participant)).toList();
    }

    public boolean hasJoined(PhoneNumber phoneNumber) {
        return participants.stream()
                .anyMatch(participant -> participant.getPhoneNumber().equals(phoneNumber));
    }

    // 휴대폰은 모임 안에서 중복되지 않으므로, 저장 뒤 id가 채워진 참여자를 휴대폰으로 찾는다.
    public Participant getParticipant(PhoneNumber phoneNumber) {
        return participants.stream()
                .filter(participant -> participant.getPhoneNumber().equals(phoneNumber))
                .findFirst()
                .orElseThrow(ParticipantNotFoundException::new);
    }

    public List<Participant> getParticipants() {
        return List.copyOf(participants);
    }
}
