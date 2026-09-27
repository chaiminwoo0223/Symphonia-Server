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
    @DisplayName("findAll 메서드는")
    class FindAll {

        @Test
        @DisplayName("저장된 모든 Drink를 반환한다")
        void shouldReturnDrinks() {
            // given
            DrinkJpaEntity saved = saveDrink(DrinkFixture.SOJU);

            // when
            List<Drink> result = drinkRepository.findAll();

            // then
            assertThat(result).extracting(Drink::getId).contains(saved.getId());
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
    @DisplayName("saveAndFlush 메서드는 (source, externalId) 유니크 제약에 따라")
    class SaveAndFlush {

        @Test
        @DisplayName("같은 source와 externalId를 가진 Drink를 두 번 저장할 수 없다")
        void shouldThrowDataIntegrityViolationException() {
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
