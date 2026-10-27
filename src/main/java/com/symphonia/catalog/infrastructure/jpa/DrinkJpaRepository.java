package com.symphonia.catalog.infrastructure.jpa;

import com.symphonia.catalog.domain.vo.DrinkSource;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DrinkJpaRepository extends JpaRepository<DrinkJpaEntity, Long> {
    List<DrinkJpaEntity> findAllByAbvLessThanEqual(double abv);

    List<DrinkJpaEntity> findAllBySource(DrinkSource source);
}
