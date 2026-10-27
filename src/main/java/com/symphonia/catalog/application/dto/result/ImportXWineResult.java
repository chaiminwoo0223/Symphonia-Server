package com.symphonia.catalog.application.dto.result;

import com.symphonia.catalog.domain.vo.WineAttribute;
import com.symphonia.catalog.domain.vo.WineEvaluation;
import com.symphonia.catalog.domain.vo.XWine;
import java.util.List;
import java.util.Set;

public record ImportXWineResult(
        int importedStyleCount,
        int importedDrinkCount,
        List<AbvExceededWine> abvExceededWines,
        List<UndeterminedWine> undeterminedWines) {
    public ImportXWineResult {
        abvExceededWines = List.copyOf(abvExceededWines);
        undeterminedWines = List.copyOf(undeterminedWines);
    }

    // 도수 상한을 넘은 와인과 맛을 정하지 못한 와인은 적재하지 않고 제외 항목으로 남긴다.
    public static ImportXWineResult of(
            int importedStyleCount,
            List<WineEvaluation> determined,
            List<XWine> abvExceeded,
            List<WineEvaluation> undetermined) {
        return new ImportXWineResult(
                importedStyleCount,
                determined.size(),
                abvExceeded.stream().map(AbvExceededWine::from).toList(),
                undetermined.stream().map(UndeterminedWine::from).toList());
    }

    public record AbvExceededWine(String externalId, String name, double abv) {
        public static AbvExceededWine from(XWine wine) {
            return new AbvExceededWine(wine.wineId(), wine.displayName(), wine.abv());
        }
    }

    public record UndeterminedWine(
            String externalId, String name, Set<WineAttribute> unknownAttributes) {
        public static UndeterminedWine from(WineEvaluation evaluation) {
            return new UndeterminedWine(
                    evaluation.wine().wineId(),
                    evaluation.wine().displayName(),
                    evaluation.unknownAttributes());
        }
    }
}
