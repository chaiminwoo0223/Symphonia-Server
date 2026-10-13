package com.symphonia.pairing.application.dto.result;

import com.symphonia.pairing.domain.vo.WineAttribute;
import com.symphonia.pairing.domain.vo.WineEvaluation;
import com.symphonia.pairing.domain.vo.XWine;
import java.util.List;
import java.util.Set;

public record ImportXWineResult(
        int importedStyleCount,
        int importedDrinkCount,
        List<AbvExceededItem> abvExceededItems,
        List<UndeterminedItem> undeterminedItems) {
    public ImportXWineResult {
        abvExceededItems = List.copyOf(abvExceededItems);
        undeterminedItems = List.copyOf(undeterminedItems);
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
                abvExceeded.stream().map(AbvExceededItem::from).toList(),
                undetermined.stream().map(UndeterminedItem::from).toList());
    }

    public record AbvExceededItem(String externalId, String name, double abv) {
        public static AbvExceededItem from(XWine wine) {
            return new AbvExceededItem(wine.wineId(), wine.displayName(), wine.abv());
        }
    }

    public record UndeterminedItem(
            String externalId, String name, Set<WineAttribute> unknownAttributes) {
        public static UndeterminedItem from(WineEvaluation evaluation) {
            return new UndeterminedItem(
                    evaluation.wine().wineId(),
                    evaluation.wine().displayName(),
                    evaluation.unknownAttributes());
        }
    }
}
