package com.symphonia.catalog.domain.reader;

import com.symphonia.catalog.domain.vo.BeerAllergyRule;
import java.util.List;

public interface BeerAllergyRuleReader {
    List<BeerAllergyRule> read();
}
