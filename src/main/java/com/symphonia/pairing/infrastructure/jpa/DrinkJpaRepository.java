package com.symphonia.pairing.infrastructure.jpa;

import com.symphonia.pairing.domain.vo.DrinkSource;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DrinkJpaRepository extends JpaRepository<DrinkJpaEntity, Long> {
    // 추천은 음료마다 Harmonize를 보므로 지연 로딩으로 음료 수만큼 SELECT가 나가지 않게 함께 불러온다.
    @Query("select d from DrinkJpaEntity d left join fetch d.harmonizes" + " where d.abv <= :abv")
    List<DrinkJpaEntity> findAllByAbvLessThanEqual(@Param("abv") double abv);

    // 적재는 출처의 음료를 한 번에 불러와 덮어쓰므로 Harmonize도 함께 불러온다.
    @Query(
            "select d from DrinkJpaEntity d left join fetch d.harmonizes"
                    + " where d.source = :source")
    List<DrinkJpaEntity> findAllBySource(@Param("source") DrinkSource source);
}
