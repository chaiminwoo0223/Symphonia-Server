package com.symphonia.pairing.infrastructure.jpa;

import com.symphonia.pairing.domain.entity.Drink;
import com.symphonia.pairing.domain.repository.DrinkRepository;
import com.symphonia.pairing.domain.vo.DrinkSource;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DrinkRepositoryImpl implements DrinkRepository {
    private final DrinkJpaRepository drinkJpaRepository;

    @Override
    public Drink save(Drink drink) {
        DrinkJpaEntity savedEntity = drinkJpaRepository.save(DrinkJpaEntity.from(drink));

        return savedEntity.toDomain();
    }

    @Override
    public List<Drink> findAll() {
        return drinkJpaRepository.findAll().stream().map(DrinkJpaEntity::toDomain).toList();
    }

    @Override
    public Optional<Drink> findById(Long drinkId) {
        return drinkJpaRepository.findById(drinkId).map(DrinkJpaEntity::toDomain);
    }

    @Override
    public Optional<Drink> findBySourceAndExternalId(DrinkSource source, String externalId) {
        return drinkJpaRepository
                .findBySourceAndExternalId(source, externalId)
                .map(DrinkJpaEntity::toDomain);
    }
}
