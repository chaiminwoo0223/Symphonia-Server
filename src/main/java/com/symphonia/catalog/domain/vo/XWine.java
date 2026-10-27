package com.symphonia.catalog.domain.vo;

import java.util.List;

// X-Wines 데이터셋의 와인 한 행이다. type, body, acidity, harmonizeTags는 원본의 영문 값을 그대로 담는다.
public record XWine(
        String wineId,
        String wineName,
        String wineryName,
        String type,
        double abv,
        String body,
        String acidity,
        List<String> harmonizeTags) {
    // 원본에는 그라파 같은 증류주가 Dessert로 분류된 행(도수 38~50%)이 있다. 주정강화 와인(최대 21%)의 범위를 넘는 행은 와인으로 보지 않는다.
    private static final double MAX_ABV = 25.0;

    public XWine {
        harmonizeTags = List.copyOf(harmonizeTags);
    }

    public boolean exceedsAbvLimit() {
        return abv > MAX_ABV;
    }

    // 와인명만으로는 같은 이름의 와인을 구분할 수 없어서 와이너리명을 앞에 붙인다.
    public String displayName() {
        return wineryName + " " + wineName;
    }
}
