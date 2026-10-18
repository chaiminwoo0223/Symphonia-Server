package com.symphonia.pairing.infrastructure.jpa;

import com.symphonia.pairing.domain.vo.DrinkStyleSource;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DrinkStyleJpaRepository extends JpaRepository<DrinkStyleJpaEntity, Long> {
    List<DrinkStyleJpaEntity> findAllBySource(DrinkStyleSource source);
}
