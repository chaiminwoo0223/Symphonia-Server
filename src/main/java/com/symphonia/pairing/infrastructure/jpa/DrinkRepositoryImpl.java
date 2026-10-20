package com.symphonia.pairing.infrastructure.jpa;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toCollection;

import com.symphonia.pairing.domain.entity.Drink;
import com.symphonia.pairing.domain.repository.DrinkRepository;
import com.symphonia.pairing.domain.vo.DrinkSource;
import com.symphonia.pairing.domain.vo.Harmonize;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
    public List<Drink> findAllByAbvLessThanEqual(double maxAbv) {
        Map<Long, Set<Harmonize>> harmonizesByDrinkId =
                drinkJpaRepository.findHarmonizeRowsByAbvLessThanEqual(maxAbv).stream()
                        .collect(
                                groupingBy(
                                        DrinkHarmonizeRow::drinkId,
                                        mapping(
                                                DrinkHarmonizeRow::harmonize,
                                                toCollection(
                                                        () -> EnumSet.noneOf(Harmonize.class)))));

        return drinkJpaRepository.findAllByAbvLessThanEqual(maxAbv).stream()
                .map(
                        entity ->
                                entity.toDomain(
                                        harmonizesByDrinkId.getOrDefault(entity.getId(), Set.of())))
                .toList();
    }

    @Override
    public Optional<Drink> findById(Long drinkId) {
        return drinkJpaRepository.findById(drinkId).map(DrinkJpaEntity::toDomain);
    }

    @Override
    public List<Drink> findAllBySource(DrinkSource source) {
        return drinkJpaRepository.findAllBySource(source).stream()
                .map(DrinkJpaEntity::toDomain)
                .toList();
    }
}
