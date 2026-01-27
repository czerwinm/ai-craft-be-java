package devices.installation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class WorkOrderIdTest {

  @Test
  void shouldCreateValidWorkOrderId() {
    // When
    WorkOrderId id = WorkOrderId.of("WO-12345");

    // Then
    assertThat(id.value()).isEqualTo("WO-12345");
    assertThat(id.toString()).isEqualTo("WO-12345");
  }

  @Test
  void shouldFailWhenValueIsNull() {
    // When & Then
    assertThatThrownBy(() -> WorkOrderId.of(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("cannot be null or blank");
  }

  @Test
  void shouldFailWhenValueIsBlank() {
    // When & Then
    assertThatThrownBy(() -> WorkOrderId.of("   "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("cannot be null or blank");
  }

  @Test
  void shouldUseValueEquality() {
    // Given
    WorkOrderId id1 = WorkOrderId.of("WO-12345");
    WorkOrderId id2 = WorkOrderId.of("WO-12345");

    // Then
    assertThat(id1).isEqualTo(id2);
  }
}
