package com.symphonia.catalog.domain.reader;

import com.symphonia.catalog.domain.vo.CuratedDrink;
import java.util.List;

public interface CuratedDrinkReader {
    List<CuratedDrink> read();
}
