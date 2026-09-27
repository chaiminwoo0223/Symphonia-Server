package com.symphonia.pairing.infrastructure.jpa;

import com.symphonia.pairing.domain.vo.DrinkStyleSource;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DrinkStyleJpaRepository extends JpaRepository<DrinkStyleJpaEntity, Long> {
    Optional<DrinkStyleJpaEntity> findBySourceAndExternalId(
            DrinkStyleSource source, String externalId);
}
