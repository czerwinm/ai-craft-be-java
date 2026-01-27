package devices.installation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class BootDetailsTest {

  @Test
  void shouldCreateValidBootDetails() {
    // When
    BootDetails boot = BootDetails.of("Garo", "CPF25 Family", "1.1", "891234A56711", "IoT16");

    // Then
    assertThat(boot.vendor()).isEqualTo("Garo");
    assertThat(boot.model()).isEqualTo("CPF25 Family");
    assertThat(boot.firmware()).isEqualTo("1.1");
    assertThat(boot.serial()).isEqualTo("891234A56711");
    assertThat(boot.protocol()).isEqualTo("IoT16");
  }

  @Test
  void shouldDetectDifferentHardware() {
    // Given
    BootDetails boot1 = BootDetails.of("Garo", "CPF25 Family", "1.1", "891234A56711", "IoT16");
    BootDetails boot2 = BootDetails.of("Garo", "CPF25 Family", "1.1", "999999X99999", "IoT16");

    // Then
    assertThat(boot1.isDifferentHardwareFrom(boot2)).isTrue();
  }

  @Test
  void shouldDetectSameHardware() {
    // Given
    BootDetails boot1 = BootDetails.of("Garo", "CPF25 Family", "1.1", "891234A56711", "IoT16");
    BootDetails boot2 = BootDetails.of("Garo", "CPF25 Family", "1.2", "891234A56711", "IoT16");

    // Then
    assertThat(boot1.isDifferentHardwareFrom(boot2)).isFalse();
  }

  @Test
  void shouldDetectDifferentFirmware() {
    // Given
    BootDetails boot1 = BootDetails.of("Garo", "CPF25 Family", "1.1", "891234A56711", "IoT16");
    BootDetails boot2 = BootDetails.of("Garo", "CPF25 Family", "1.13", "891234A56711", "IoT16");

    // Then
    assertThat(boot1.hasDifferentFirmwareFrom(boot2)).isTrue();
  }

  @Test
  void shouldDetectSameFirmware() {
    // Given
    BootDetails boot1 = BootDetails.of("Garo", "CPF25 Family", "1.1", "891234A56711", "IoT16");
    BootDetails boot2 = BootDetails.of("Garo", "CPF25 Family", "1.1", "891234A56711", "IoT16");

    // Then
    assertThat(boot1.hasDifferentFirmwareFrom(boot2)).isFalse();
  }

  @Test
  void shouldFailWhenVendorIsNull() {
    // When & Then
    assertThatThrownBy(() -> BootDetails.of(null, "Model", "1.0", "Serial", "IoT16"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Vendor");
  }

  @Test
  void shouldFailWhenSerialIsNull() {
    // When & Then
    assertThatThrownBy(() -> BootDetails.of("Vendor", "Model", "1.0", null, "IoT16"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Serial");
  }
}
