package com.symphonia.pairing.domain.reader;

import com.symphonia.pairing.domain.vo.CuratedDrink;
import java.util.List;

public interface CuratedDrinkReader {
    List<CuratedDrink> read();
}
