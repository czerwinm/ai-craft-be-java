package devices.configuration.management;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OpeningHoursTest {

  @Test
  void shouldCreateAlwaysOpenOpeningHours() {
    // Given & When
    OpeningHours openingHours = OpeningHours.alwaysOpened();

    // Then
    assertThat(openingHours.alwaysOpen()).isTrue();
  }

  @Test
  void shouldCreateCustomOpeningHours() {
    // Given & When
    OpeningHours openingHours = OpeningHours.of(false);

    // Then
    assertThat(openingHours.alwaysOpen()).isFalse();
  }

  @Test
  void shouldUseValueEquality() {
    // Given
    OpeningHours openingHours1 = OpeningHours.alwaysOpened();
    OpeningHours openingHours2 = OpeningHours.alwaysOpened();

    // When & Then
    assertThat(openingHours1).isEqualTo(openingHours2);
  }
}
