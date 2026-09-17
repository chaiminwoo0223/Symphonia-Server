package com.symphonia.pairing.infrastructure.jpa;

import com.symphonia.pairing.domain.entity.Drink;
import com.symphonia.pairing.domain.repository.DrinkRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DrinkRepositoryImpl implements DrinkRepository {
    private final DrinkJpaRepository drinkJpaRepository;

    @Override
    public List<Drink> findAll() {
        return drinkJpaRepository.findAll().stream().map(DrinkJpaEntity::toDomain).toList();
    }

    @Override
    public Optional<Drink> findById(Long drinkId) {
        return drinkJpaRepository.findById(drinkId).map(DrinkJpaEntity::toDomain);
    }
}
