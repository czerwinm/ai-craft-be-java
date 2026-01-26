package devices.configuration.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CoordinatesTest {

  @Test
  void shouldCreateValidCoordinates() {
    // Given & When
    Coordinates coordinates = Coordinates.of(16.931752852309156, 51.09836221719513);

    // Then
    assertThat(coordinates.longitude()).isEqualTo(16.931752852309156);
    assertThat(coordinates.latitude()).isEqualTo(51.09836221719513);
  }

  @Test
  void shouldUseValueEquality() {
    // Given
    Coordinates coordinates1 = Coordinates.of(16.931752852309156, 51.09836221719513);
    Coordinates coordinates2 = Coordinates.of(16.931752852309156, 51.09836221719513);

    // When & Then
    assertThat(coordinates1).isEqualTo(coordinates2);
  }

  @Test
  void shouldFailWhenLatitudeTooHigh() {
    // When & Then
    assertThatThrownBy(() -> Coordinates.of(16.0, 91.0))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldFailWhenLatitudeTooLow() {
    // When & Then
    assertThatThrownBy(() -> Coordinates.of(16.0, -91.0))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldFailWhenLongitudeTooHigh() {
    // When & Then
    assertThatThrownBy(() -> Coordinates.of(181.0, 51.0))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldFailWhenLongitudeTooLow() {
    // When & Then
    assertThatThrownBy(() -> Coordinates.of(-181.0, 51.0))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
