package com.symphonia.gathering.infrastructure.jpa;

import com.symphonia.gathering.infrastructure.crypto.GatheringMasterKey.WrappedDataKey;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "gathering_data_key")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class GatheringDataKeyJpaEntity {
    @Id private Long gatheringId;

    @Column(nullable = false)
    private byte[] encryptedCipherKey;

    @Column(nullable = false)
    private byte[] encryptedHmacKey;

    public static GatheringDataKeyJpaEntity of(Long gatheringId, WrappedDataKey wrappedDataKey) {
        return new GatheringDataKeyJpaEntity(
                gatheringId,
                wrappedDataKey.encryptedCipherKey(),
                wrappedDataKey.encryptedHmacKey());
    }

    public WrappedDataKey toWrappedDataKey() {
        return new WrappedDataKey(encryptedCipherKey, encryptedHmacKey);
    }
}
