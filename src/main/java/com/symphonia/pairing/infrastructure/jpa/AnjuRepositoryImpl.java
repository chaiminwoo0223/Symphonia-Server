package com.symphonia.pairing.infrastructure.jpa;

import com.symphonia.pairing.domain.entity.Anju;
import com.symphonia.pairing.domain.repository.AnjuRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AnjuRepositoryImpl implements AnjuRepository {
    private final AnjuJpaRepository anjuJpaRepository;

    @Override
    public List<Anju> findAll() {
        return anjuJpaRepository.findAll().stream().map(AnjuJpaEntity::toDomain).toList();
    }

    @Override
    public Optional<Anju> findById(Long anjuId) {
        return anjuJpaRepository.findById(anjuId).map(AnjuJpaEntity::toDomain);
    }
}
