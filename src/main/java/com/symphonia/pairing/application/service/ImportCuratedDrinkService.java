package com.symphonia.pairing.application.service;

import static java.util.stream.Collectors.toMap;

import com.symphonia.common.annotation.CommandService;
import com.symphonia.pairing.application.dto.result.ImportCuratedDrinkResult;
import com.symphonia.pairing.application.dto.result.ImportCuratedDrinkResult.UnmappedItem;
import com.symphonia.pairing.application.usecase.ImportCuratedDrinkUseCase;
import com.symphonia.pairing.domain.entity.Drink;
import com.symphonia.pairing.domain.entity.DrinkStyle;
import com.symphonia.pairing.domain.reader.CuratedDrinkReader;
import com.symphonia.pairing.domain.repository.DrinkRepository;
import com.symphonia.pairing.domain.repository.DrinkStyleRepository;
import com.symphonia.pairing.domain.vo.CuratedDrink;
import com.symphonia.pairing.domain.vo.DrinkSource;
import com.symphonia.pairing.domain.vo.DrinkStyleSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;

@CommandService
@RequiredArgsConstructor
public class ImportCuratedDrinkService implements ImportCuratedDrinkUseCase {
    private final CuratedDrinkReader curatedDrinkReader;
    private final DrinkStyleRepository drinkStyleRepository;
    private final DrinkRepository drinkRepository;

    // 매핑한 BJCP 스타일이 없으면(아직 적재되지 않았거나 맛을 정할 수 없어 스타일 적재에서 제외된 경우) 제품을 저장하지 않고 결과에 남긴다.
    // 이미 적재된 제품은 (source, external_id)로 찾아 갱신하므로 여러 번 실행해도 결과가 같다.
    // 스타일과 기존 제품은 제품마다 조회하지 않고 한 번에 불러와 external_id로 찾는다.
    @Override
    public ImportCuratedDrinkResult importCuratedBeers() {
        List<CuratedDrink> curatedDrinks = curatedDrinkReader.read();
        Map<String, DrinkStyle> stylesByExternalId =
                drinkStyleRepository.findAllBySource(DrinkStyleSource.BJCP).stream()
                        .collect(toMap(DrinkStyle::getExternalId, Function.identity()));
        Map<String, Drink> existingByExternalId =
                drinkRepository.findAllBySource(DrinkSource.CURATED).stream()
                        .collect(toMap(Drink::getExternalId, Function.identity()));
        List<UnmappedItem> unmappedItems = new ArrayList<>();

        curatedDrinks.forEach(
                curatedDrink ->
                        Optional.ofNullable(stylesByExternalId.get(curatedDrink.bjcpStyleId()))
                                .map(curatedDrink::toDrink)
                                .ifPresentOrElse(
                                        drink -> upsert(drink, existingByExternalId),
                                        () -> unmappedItems.add(UnmappedItem.from(curatedDrink))));

        return ImportCuratedDrinkResult.of(curatedDrinks.size(), unmappedItems);
    }

    private void upsert(Drink imported, Map<String, Drink> existingByExternalId) {
        Drink drink =
                Optional.ofNullable(existingByExternalId.get(imported.getExternalId()))
                        .map(existing -> existing.overwriteWith(imported))
                        .orElse(imported);
        drinkRepository.save(drink);
    }
}
