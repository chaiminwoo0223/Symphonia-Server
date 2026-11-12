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

    // 안주를 FK로 참조하는 테이블은 pairing 도메인의 pairing_feedback과 조합별 집계 두 개다.
    // 조합별 집계는 pairing_rating_summary와 pairing_situation_rating_summary다.
    // 집계 행은 피드백이 있는 조합에만 생기고 피드백을 지우는 경로가 없으므로 pairing_feedback만 확인한다.
    // catalog가 pairing 코드를 의존하지 않도록 엔티티 대신 테이블을 직접 조회한다.
    @Query(
            value = "select distinct anju_id from pairing_feedback where anju_id in (:anjuIds)",
            nativeQuery = true)
    Set<Long> findReferencedIds(@Param("anjuIds") Collection<Long> anjuIds);
}
