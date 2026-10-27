package com.symphonia.catalog.infrastructure.jpa;

import com.symphonia.catalog.domain.entity.DrinkStyle;
import com.symphonia.catalog.domain.repository.DrinkStyleRepository;
import com.symphonia.catalog.domain.vo.DrinkStyleSource;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DrinkStyleRepositoryImpl implements DrinkStyleRepository {
    private final DrinkStyleJpaRepository drinkStyleJpaRepository;

    @Override
    public DrinkStyle save(DrinkStyle drinkStyle) {
        DrinkStyleJpaEntity savedEntity =
                drinkStyleJpaRepository.save(DrinkStyleJpaEntity.from(drinkStyle));

        return savedEntity.toDomain();
    }

    @Override
    public Optional<DrinkStyle> findById(Long drinkStyleId) {
        return drinkStyleJpaRepository.findById(drinkStyleId).map(DrinkStyleJpaEntity::toDomain);
    }

    @Override
    public List<DrinkStyle> findAllBySource(DrinkStyleSource source) {
        return drinkStyleJpaRepository.findAllBySource(source).stream()
                .map(DrinkStyleJpaEntity::toDomain)
                .toList();
    }
}
