package com.symphonia.pairing.infrastructure.jpa;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AnjuJpaRepository extends JpaRepository<AnjuJpaEntity, Long> {
    @Query("select a from AnjuJpaEntity a left join fetch a.allergyTypes")
    List<AnjuJpaEntity> findAllWithAllergyTypes();
}
