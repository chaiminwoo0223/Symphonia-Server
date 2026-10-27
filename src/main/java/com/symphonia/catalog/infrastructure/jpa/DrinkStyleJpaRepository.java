package com.symphonia.catalog.infrastructure.jpa;

import com.symphonia.catalog.domain.vo.DrinkStyleSource;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DrinkStyleJpaRepository extends JpaRepository<DrinkStyleJpaEntity, Long> {
    List<DrinkStyleJpaEntity> findAllBySource(DrinkStyleSource source);
}
