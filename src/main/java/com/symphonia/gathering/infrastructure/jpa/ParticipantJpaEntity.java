package com.symphonia.gathering.infrastructure.jpa;

import com.symphonia.common.entity.BaseTimeEntity;
import com.symphonia.gathering.domain.entity.Participant;
import com.symphonia.gathering.domain.vo.PhoneNumber;
import com.symphonia.gathering.infrastructure.crypto.GatheringDataKey;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "participant")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ParticipantJpaEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long gatheringId;

    @Column(nullable = false)
    private byte[] encryptedName;

    @Column(nullable = false)
    private byte[] encryptedPhoneNumber;

    @Column(nullable = false, length = 64)
    private String phoneNumberHmac;

    // 구글 로그인으로 참여한 참여자만 갖는다. 참여자를 저장한 뒤 ParticipantGoogleSubjectRepository가 채운다.
    @Column(length = 64)
    private String googleSubHmac;

    @Column(nullable = false, length = 20)
    private String consentVersion;

    @Column(nullable = false)
    private LocalDateTime consentedAt;

    // 이름과 휴대폰은 모임별 데이터 키로 암호화한 값만 저장한다.
    public static ParticipantJpaEntity from(
            Participant participant, Long gatheringId, GatheringDataKey dataKey) {
        String phoneNumber = participant.getPhoneNumber().value();

        return ParticipantJpaEntity.builder()
                .id(participant.getId())
                .gatheringId(gatheringId)
                .encryptedName(dataKey.encrypt(participant.getName()))
                .encryptedPhoneNumber(dataKey.encrypt(phoneNumber))
                .phoneNumberHmac(dataKey.hmac(phoneNumber))
                .consentVersion(participant.getConsentVersion())
                .consentedAt(participant.getConsentedAt())
                .build();
    }

    public Participant toDomain(GatheringDataKey dataKey) {
        return Participant.builder()
                .id(id)
                .name(dataKey.decrypt(encryptedName))
                .phoneNumber(new PhoneNumber(dataKey.decrypt(encryptedPhoneNumber)))
                .consentVersion(consentVersion)
                .consentedAt(consentedAt)
                .build();
    }
}
