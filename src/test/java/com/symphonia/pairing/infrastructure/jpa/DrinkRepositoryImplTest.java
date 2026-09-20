package com.symphonia.pairing.infrastructure.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import com.symphonia.RepositoryTest;
import com.symphonia.pairing.domain.entity.Drink;
import com.symphonia.pairing.domain.repository.DrinkRepository;
import com.symphonia.pairing.fixture.DrinkFixture;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(DrinkRepositoryImpl.class)
class DrinkRepositoryImplTest extends RepositoryTest {

    @Autowired private DrinkRepository drinkRepository;
    @Autowired private DrinkJpaRepository drinkJpaRepository;

    @Nested
    @DisplayName("findAll 메서드는")
    class FindAll {

        @Test
        @DisplayName("저장된 모든 Drink를 반환한다")
        void shouldReturnDrinks() {
            // given
            DrinkJpaEntity saved =
                    drinkJpaRepository.save(DrinkJpaEntity.from(DrinkFixture.SOJU.create()));

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
            DrinkJpaEntity saved =
                    drinkJpaRepository.save(DrinkJpaEntity.from(DrinkFixture.SOJU.create()));

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
            DrinkJpaEntity saved =
                    drinkJpaRepository.save(DrinkJpaEntity.from(DrinkFixture.SODA.create()));

            // when
            Optional<Drink> result = drinkRepository.findById(saved.getId());

            // then
            assertThat(result).isPresent();
            assertThat(result.get().isNonAlcoholic()).isTrue();
        }
    }
}
