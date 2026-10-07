package com.symphonia.pairing.infrastructure.jpa;

import com.symphonia.pairing.domain.vo.DrinkSource;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DrinkJpaRepository extends JpaRepository<DrinkJpaEntity, Long> {
    Optional<DrinkJpaEntity> findBySourceAndExternalId(DrinkSource source, String externalId);
}
