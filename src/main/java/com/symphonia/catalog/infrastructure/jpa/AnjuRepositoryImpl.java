package com.symphonia.catalog.infrastructure.jpa;

import com.symphonia.catalog.domain.entity.Anju;
import com.symphonia.catalog.domain.repository.AnjuRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AnjuRepositoryImpl implements AnjuRepository {
    private final AnjuJpaRepository anjuJpaRepository;

    @Override
    public Anju save(Anju anju) {
        AnjuJpaEntity savedEntity = anjuJpaRepository.save(AnjuJpaEntity.from(anju));

        return savedEntity.toDomain();
    }

    @Override
    public List<Anju> findAll() {
        return anjuJpaRepository.findAllWithAllergyTypes().stream()
                .map(AnjuJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<Anju> findById(Long anjuId) {
        return anjuJpaRepository.findById(anjuId).map(AnjuJpaEntity::toDomain);
    }

    // Hibernate는 같은 트랜잭션의 INSERT를 DELETE보다 먼저 실행한다. 그래서 삭제를 바로 반영한다.
    // 그래야 지운 안주와 같은 이름의 안주를 이어서 저장해도 uk_anju_name에 걸리지 않는다.
    @Override
    public void deleteAllById(Collection<Long> anjuIds) {
        anjuJpaRepository.deleteAllById(anjuIds);
        anjuJpaRepository.flush();
    }
}
