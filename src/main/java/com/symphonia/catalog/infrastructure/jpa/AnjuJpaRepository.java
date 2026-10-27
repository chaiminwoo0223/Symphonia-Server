package com.symphonia.catalog.infrastructure.jpa;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnjuJpaRepository extends JpaRepository<AnjuJpaEntity, Long> {
    @Query("select a from AnjuJpaEntity a left join fetch a.allergyTypes")
    List<AnjuJpaEntity> findAllWithAllergyTypes();

    // 안주를 FK로 참조하는 테이블은 pairing 도메인의 pairing_feedback이다.
    // catalog가 pairing 코드를 의존하지 않도록 엔티티 대신 테이블을 직접 조회한다.
    @Query(
            value = "select distinct anju_id from pairing_feedback where anju_id in (:anjuIds)",
            nativeQuery = true)
    Set<Long> findReferencedIds(@Param("anjuIds") Collection<Long> anjuIds);
}
