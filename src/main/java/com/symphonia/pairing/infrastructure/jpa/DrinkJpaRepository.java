package com.symphonia.pairing.infrastructure.jpa;

import com.symphonia.pairing.domain.vo.DrinkSource;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DrinkJpaRepository extends JpaRepository<DrinkJpaEntity, Long> {
    List<DrinkJpaEntity> findAllByAbvLessThanEqual(double abv);

    // 추천은 요청마다 음료 전체를 읽는다. 음식 계열을 fetch join으로 함께 읽으면 음료 행이 계열 수만큼 늘고
    // 음료마다 컬렉션이 만들어져 처리량이 절반 아래로 떨어졌다(#105 측정). 그래서 계열은 행으로만 따로 읽는다.
    @Query(
            "select new com.symphonia.pairing.infrastructure.jpa.DrinkHarmonizeRow(d.id, h)"
                    + " from DrinkJpaEntity d join d.harmonizes h where d.abv <= :abv")
    List<DrinkHarmonizeRow> findHarmonizeRowsByAbvLessThanEqual(@Param("abv") double abv);

    // 적재는 출처의 음료를 한 번에 불러와 덮어쓰므로 Harmonize도 함께 불러온다.
    @Query(
            "select d from DrinkJpaEntity d left join fetch d.harmonizes"
                    + " where d.source = :source")
    List<DrinkJpaEntity> findAllBySource(@Param("source") DrinkSource source);
}
