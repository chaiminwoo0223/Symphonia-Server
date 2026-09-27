package com.symphonia.pairing.infrastructure.jpa;

import com.symphonia.pairing.domain.entity.DrinkStyle;
import com.symphonia.pairing.domain.repository.DrinkStyleRepository;
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
}
