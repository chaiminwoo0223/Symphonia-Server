package com.symphonia.pairing.domain.reader;

import com.symphonia.pairing.domain.vo.BeerHarmonizeRule;
import java.util.List;

public interface BeerHarmonizeRuleReader {
    List<BeerHarmonizeRule> read();
}
