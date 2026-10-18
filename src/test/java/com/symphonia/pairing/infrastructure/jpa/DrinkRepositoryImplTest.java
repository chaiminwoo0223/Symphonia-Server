package com.symphonia.pairing.infrastructure.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.symphonia.RepositoryTest;
import com.symphonia.pairing.domain.entity.Drink;
import com.symphonia.pairing.domain.repository.DrinkRepository;
import com.symphonia.pairing.domain.vo.DrinkSource;
import com.symphonia.pairing.fixture.DrinkFixture;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.jdbc.Sql;

@Import(DrinkRepositoryImpl.class)
@Sql("/sql/clear-pairing-catalog.sql")
class DrinkRepositoryImplTest extends RepositoryTest {

    @Autowired private DrinkRepository drinkRepository;
    @Autowired private DrinkJpaRepository drinkJpaRepository;
    @Autowired private DrinkStyleJpaRepository drinkStyleJpaRepository;

    @Nested
    @DisplayName("save 메서드는")
    class Save {

        @Test
        @DisplayName("id가 없는 Drink를 저장하고 ID를 채워 반환한다")
        void shouldPersistDrinkWhenIdNotExists() {
            // given
            Long drinkStyleId = saveDrinkStyle(DrinkFixture.BEER);
            Drink drink = DrinkFixture.BEER.createWithStyle(drinkStyleId);

            // when
            Drink saved = drinkRepository.save(drink);

            // then
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getName()).isEqualTo(DrinkFixture.BEER.getName());
        }

        @Test
        @DisplayName("id가 있는 Drink면 같은 행을 갱신한다")
        void shouldUpdateDrinkWhenIdExists() {
            // given
            Long drinkStyleId = saveDrinkStyle(DrinkFixture.BEER);
            Drink saved = drinkRepository.save(DrinkFixture.BEER.createWithStyle(drinkStyleId));
            saved.overwriteWith(
                    DrinkFixture.WINE.createWithStyleAndExternalId(
                            drinkStyleId, saved.getExternalId()));

            // when
            Drink updated = drinkRepository.save(saved);

            // then
            Optional<Drink> result = drinkRepository.findById(saved.getId());
            assertThat(updated.getId()).isEqualTo(saved.getId());
            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo(DrinkFixture.WINE.getName());
            assertThat(result.get().getAbv()).isEqualTo(DrinkFixture.WINE.getAbv());
            assertThat(result.get().getFlavorProfile())
                    .usingRecursiveComparison()
                    .isEqualTo(DrinkFixture.WINE.getFlavorProfile());
        }
    }

    @Nested
    @DisplayName("findAllByAbvLessThanEqual 메서드는")
    class FindAllByAbvLessThanEqual {

        @Test
        @DisplayName("도수가 상한 이하인 Drink만 반환하고 상한과 같은 도수도 포함한다")
        void shouldReturnDrinksWithAbvAtMostMaxAbv() {
            // given
            DrinkJpaEntity beer = saveDrink(DrinkFixture.BEER);
            DrinkJpaEntity wine = saveDrink(DrinkFixture.WINE);
            saveDrink(DrinkFixture.SOJU);
            saveDrink(DrinkFixture.WHISKEY);

            // when
            List<Drink> result =
                    drinkRepository.findAllByAbvLessThanEqual(DrinkFixture.WINE.getAbv());

            // then
            assertThat(result)
                    .extracting(Drink::getId)
                    .containsExactlyInAnyOrder(beer.getId(), wine.getId());
        }

        @Test
        @DisplayName("상한이 Double.MAX_VALUE면 모든 Drink를 반환한다")
        void shouldReturnDrinksWhenMaxAbvIsUnlimited() {
            // given
            DrinkJpaEntity soju = saveDrink(DrinkFixture.SOJU);
            DrinkJpaEntity whiskey = saveDrink(DrinkFixture.WHISKEY);

            // when
            List<Drink> result = drinkRepository.findAllByAbvLessThanEqual(Double.MAX_VALUE);

            // then
            assertThat(result)
                    .extracting(Drink::getId)
                    .containsExactlyInAnyOrder(soju.getId(), whiskey.getId());
        }
    }

    @Nested
    @DisplayName("findById 메서드는")
    class FindById {

        @Test
        @DisplayName("존재하는 ID면 Drink를 반환한다")
        void shouldReturnDrinkWhenIdExists() {
            // given
            DrinkJpaEntity saved = saveDrink(DrinkFixture.SOJU);

            // when
            Optional<Drink> result = drinkRepository.findById(saved.getId());

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo(DrinkFixture.SOJU.getName());
        }

        @Test
        @DisplayName("존재하지 않는 ID면 빈 Optional을 반환한다")
        void shouldReturnEmptyWhenIdNotExists() {
            // when
            Optional<Drink> result = drinkRepository.findById(-1L);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("nonAlcoholic 값을 포함해 Drink를 반환한다")
        void shouldReturnDrinkWithNonAlcoholicFlag() {
            // given
            DrinkJpaEntity saved = saveDrink(DrinkFixture.SODA);

            // when
            Optional<Drink> result = drinkRepository.findById(saved.getId());

            // then
            assertThat(result).isPresent();
            assertThat(result.get().isNonAlcoholic()).isTrue();
        }

        @Test
        @DisplayName("flavorProfile의 acidity 값을 포함해 Drink를 반환한다")
        void shouldReturnDrinkWithAcidity() {
            // given
            DrinkJpaEntity saved = saveDrink(DrinkFixture.WINE);

            // when
            Optional<Drink> result = drinkRepository.findById(saved.getId());

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getFlavorProfile().getAcidity())
                    .isEqualTo(DrinkFixture.WINE.getFlavorProfile().getAcidity());
        }

        @Test
        @DisplayName("drinkStyleId, source, externalId 값을 포함해 Drink를 반환한다")
        void shouldReturnDrinkWithStyleAndSource() {
            // given
            DrinkJpaEntity saved = saveDrink(DrinkFixture.BEER);

            // when
            Optional<Drink> result = drinkRepository.findById(saved.getId());

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getDrinkStyleId()).isEqualTo(saved.getDrinkStyleId());
            assertThat(result.get().getSource()).isEqualTo(DrinkSource.SEED);
            assertThat(result.get().getExternalId()).isEqualTo(DrinkFixture.BEER.getExternalId());
        }
    }

    @Nested
    @DisplayName("findAllBySource 메서드는")
    class FindAllBySource {

        @Test
        @DisplayName("source가 일치하는 Drink를 반환한다")
        void shouldReturnDrinksWhenSourceMatches() {
            // given
            DrinkJpaEntity saved = saveDrink(DrinkFixture.BEER);

            // when
            List<Drink> result = drinkRepository.findAllBySource(DrinkSource.SEED);

            // then
            assertThat(result)
                    .filteredOn(drink -> drink.getId().equals(saved.getId()))
                    .singleElement()
                    .extracting(Drink::getExternalId)
                    .isEqualTo(DrinkFixture.BEER.getExternalId());
        }

        @Test
        @DisplayName("source가 다른 Drink는 반환하지 않는다")
        void shouldExcludeDrinksWhenSourceDiffers() {
            // given
            DrinkJpaEntity saved = saveDrink(DrinkFixture.BEER);

            // when
            List<Drink> result = drinkRepository.findAllBySource(DrinkSource.CURATED);

            // then
            assertThat(result).extracting(Drink::getId).doesNotContain(saved.getId());
        }
    }

    @Nested
    @DisplayName("saveAndFlush 메서드는")
    class SaveAndFlush {

        @Test
        @DisplayName("같은 source와 externalId를 가진 Drink를 두 번 저장할 수 없다")
        void shouldThrowDataIntegrityViolationExceptionWhenSourceAndExternalIdDuplicated() {
            // given
            Long drinkStyleId = saveDrinkStyle(DrinkFixture.SOJU);
            drinkJpaRepository.saveAndFlush(
                    DrinkJpaEntity.from(DrinkFixture.SOJU.createWithStyle(drinkStyleId)));

            // when & then
            assertThatThrownBy(
                            () ->
                                    drinkJpaRepository.saveAndFlush(
                                            DrinkJpaEntity.from(
                                                    DrinkFixture.SOJU.createWithStyle(
                                                            drinkStyleId))))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    private DrinkJpaEntity saveDrink(DrinkFixture drinkFixture) {
        Long drinkStyleId = saveDrinkStyle(drinkFixture);

        return drinkJpaRepository.save(
                DrinkJpaEntity.from(drinkFixture.createWithStyle(drinkStyleId)));
    }

    private Long saveDrinkStyle(DrinkFixture drinkFixture) {
        return drinkStyleJpaRepository
                .save(DrinkStyleJpaEntity.from(drinkFixture.getDrinkStyleFixture().create()))
                .getId();
    }
}
