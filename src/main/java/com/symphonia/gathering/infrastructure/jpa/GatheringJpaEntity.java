package com.symphonia.gathering.infrastructure.jpa;

import com.symphonia.common.entity.BaseTimeEntity;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.entity.GatheringRound;
import com.symphonia.gathering.domain.entity.Participant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "gathering")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GatheringJpaEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long hostMemberId;

    // 참여나 차수 추가로 모임을 저장할 때 메모리에 있던 옛 이름으로 되돌리지 않도록 저장으로는 바꾸지 않는다.
    // 이름은 GatheringJpaRepository.updateName으로만 바꾼다.
    @Column(nullable = false, updatable = false)
    private String name;

    @Column(nullable = false, updatable = false)
    private LocalDateTime openedAt;

    // 참여나 차수 추가로 모임을 저장할 때 메모리에 있던 옛 값으로 연장 여부를 되돌리지 않도록 저장으로는 바꾸지 않는다.
    // 연장은 GatheringJpaRepository.markRecordingExtended로만 바꾼다.
    @Column(nullable = false, updatable = false)
    private boolean recordingExtended;

    public static GatheringJpaEntity from(Gathering gathering) {
        return GatheringJpaEntity.builder()
                .id(gathering.getId())
                .hostMemberId(gathering.getHostMemberId())
                .name(gathering.getName())
                .openedAt(gathering.getOpenedAt())
                .recordingExtended(gathering.isRecordingExtended())
                .build();
    }

    // 참여자와 차수는 별도 테이블에서 읽어 받는다. 참여자는 복호화한 뒤 받는다.
    public Gathering toDomain(List<Participant> participants, List<GatheringRound> rounds) {
        return Gathering.builder()
                .id(id)
                .hostMemberId(hostMemberId)
                .name(name)
                .openedAt(openedAt)
                .recordingExtended(recordingExtended)
                .participants(participants)
                .rounds(rounds)
                .build();
    }
}
