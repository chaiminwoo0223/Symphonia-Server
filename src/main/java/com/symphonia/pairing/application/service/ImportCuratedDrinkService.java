package com.symphonia.pairing.application.service;

import com.symphonia.common.annotation.CommandService;
import com.symphonia.pairing.application.dto.result.ImportCuratedDrinkResult;
import com.symphonia.pairing.application.dto.result.ImportCuratedDrinkResult.UnmappedItem;
import com.symphonia.pairing.application.usecase.ImportCuratedDrinkUseCase;
import com.symphonia.pairing.domain.entity.Drink;
import com.symphonia.pairing.domain.reader.CuratedDrinkReader;
import com.symphonia.pairing.domain.repository.DrinkRepository;
import com.symphonia.pairing.domain.repository.DrinkStyleRepository;
import com.symphonia.pairing.domain.vo.CuratedDrink;
import com.symphonia.pairing.domain.vo.DrinkStyleSource;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

@CommandService
@RequiredArgsConstructor
public class ImportCuratedDrinkService implements ImportCuratedDrinkUseCase {
    private final CuratedDrinkReader curatedDrinkReader;
    private final DrinkStyleRepository drinkStyleRepository;
    private final DrinkRepository drinkRepository;

    // 매핑한 BJCP 스타일이 없으면(아직 적재되지 않았거나 맛을 정할 수 없어 스타일 적재에서 제외된 경우) 제품을 저장하지 않고 결과에 남긴다.
    // 이미 적재된 제품은 (source, external_id)로 찾아 갱신하므로 여러 번 실행해도 결과가 같다.
    @Override
    public ImportCuratedDrinkResult importCuratedBeers() {
        List<CuratedDrink> curatedDrinks = curatedDrinkReader.read();
        List<UnmappedItem> unmappedItems = new ArrayList<>();

        curatedDrinks.forEach(
                curatedDrink ->
                        drinkStyleRepository
                                .findBySourceAndExternalId(
                                        DrinkStyleSource.BJCP, curatedDrink.bjcpStyleId())
                                .map(curatedDrink::toDrink)
                                .ifPresentOrElse(
                                        this::upsert,
                                        () -> unmappedItems.add(UnmappedItem.from(curatedDrink))));

        return ImportCuratedDrinkResult.of(curatedDrinks.size(), unmappedItems);
    }

    private void upsert(Drink imported) {
        drinkRepository
                .findBySourceAndExternalId(imported.getSource(), imported.getExternalId())
                .ifPresentOrElse(
                        existing -> {
                            existing.update(
                                    imported.getDrinkStyleId(),
                                    imported.getName(),
                                    imported.getAbv(),
                                    imported.getFlavorProfile());
                            drinkRepository.save(existing);
                        },
                        () -> drinkRepository.save(imported));
    }
}
