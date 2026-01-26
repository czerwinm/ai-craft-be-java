package devices.configuration.management;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SettingsTest {

  @Test
  void shouldCreateDefaultSettings() {
    // Given & When
    Settings settings = Settings.defaultSettings();

    // Then
    assertThat(settings.autoStart()).isFalse();
    assertThat(settings.remoteControl()).isFalse();
    assertThat(settings.billing()).isFalse();
    assertThat(settings.reimbursement()).isFalse();
    assertThat(settings.showOnMap()).isFalse();
    assertThat(settings.publicAccess()).isFalse();
  }

  @Test
  void shouldCreateCustomSettings() {
    // Given & When
    Settings settings = Settings.of(true, false, true, false, true, false);

    // Then
    assertThat(settings.autoStart()).isTrue();
    assertThat(settings.remoteControl()).isFalse();
    assertThat(settings.billing()).isTrue();
    assertThat(settings.reimbursement()).isFalse();
    assertThat(settings.showOnMap()).isTrue();
    assertThat(settings.publicAccess()).isFalse();
  }

  @Test
  void shouldUseValueEquality() {
    // Given
    Settings settings1 = Settings.defaultSettings();
    Settings settings2 = Settings.defaultSettings();

    // When & Then
    assertThat(settings1).isEqualTo(settings2);
  }

  @Test
  void shouldSupportBuilderPattern() {
    // Given
    Settings base = Settings.defaultSettings();

    // When
    Settings modified = base.toBuilder().showOnMap(true).publicAccess(true).build();

    // Then
    assertThat(modified.showOnMap()).isTrue();
    assertThat(modified.publicAccess()).isTrue();
    assertThat(modified.autoStart()).isFalse();
    assertThat(modified.remoteControl()).isFalse();
    assertThat(modified.billing()).isFalse();
    assertThat(modified.reimbursement()).isFalse();
  }
}
