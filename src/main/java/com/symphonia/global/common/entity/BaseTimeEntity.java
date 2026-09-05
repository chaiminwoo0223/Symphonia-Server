package com.symphonia.global.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@SuperBuilder // 부모 클래스(BaseTimeEntity) 필드까지 빌더에 포함
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@MappedSuperclass // 자식 엔티티 테이블에 BaseTimeEntity 필드를 컬럼으로 매핑
@EntityListeners(
        AuditingEntityListener.class) // @CreatedDate, @LastModifiedDate 자동 주입을 위한 Auditing 리스너 등록
public abstract class BaseTimeEntity {

    @CreatedDate
    @Column(updatable = false)
    protected LocalDateTime createdAt;

    @LastModifiedDate protected LocalDateTime updatedAt;
}
