package com.symphonia.catalog.domain.repository;

import com.symphonia.catalog.domain.entity.Anju;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface AnjuRepository {
    Anju save(Anju anju);

    List<Anju> findAll();

    Optional<Anju> findById(Long anjuId);

    // 주어진 안주 중 다른 테이블이 FK로 참조해 지울 수 없는 안주의 id를 돌려준다.
    Set<Long> findReferencedIds(Collection<Long> anjuIds);

    void deleteAllById(Collection<Long> anjuIds);
}
