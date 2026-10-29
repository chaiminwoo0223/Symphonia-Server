package com.symphonia.gathering.infrastructure.jpa;

import com.symphonia.common.entity.BaseTimeEntity;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.entity.Participant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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

    @Column(nullable = false)
    private String name;

    public static GatheringJpaEntity from(Gathering gathering) {
        return GatheringJpaEntity.builder()
                .id(gathering.getId())
                .hostMemberId(gathering.getHostMemberId())
                .name(gathering.getName())
                .createdAt(gathering.getCreatedAt())
                .build();
    }

    // 참여자는 별도 테이블에서 읽어 복호화한 뒤 받는다.
    public Gathering toDomain(List<Participant> participants) {
        return Gathering.builder()
                .id(id)
                .hostMemberId(hostMemberId)
                .name(name)
                .createdAt(createdAt)
                .participants(participants)
                .build();
    }
}
