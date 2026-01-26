package devices.configuration.communication;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class IntervalCalculationIntegrationTest {

  private final IntervalConfiguration config =
      IntervalConfiguration.create(
          List.of(
              new IntervalRule.DeviceIdRule(
                  Duration.ofSeconds(600), Set.of("EVB-P4562137", "ALF-9571445")),
              new IntervalRule.DeviceIdRule(
                  Duration.ofSeconds(2700), Set.of("t53_8264_019", "EVB-P15079256"))),
          List.of(
              new IntervalRule.ModelRule(Duration.ofSeconds(60), "Alfen BV", "NG920-5250[6-9]"),
              new IntervalRule.ModelRule(
                  Duration.ofSeconds(120), "ChargeStorm AB", "Chargestorm Connected")),
          new IntervalRule.DefaultRule(Duration.ofSeconds(1800)));

  private final HeartbeatIntervalCalculator calculator = new HeartbeatIntervalCalculator(config);

  @Test
  void shouldReturn600sForDeviceEVBP4562137() {
    // Given
    BootNotification boot =
        BootNotification.builder()
            .deviceId("EVB-P4562137")
            .vendor("Alfen BV")
            .model("NG920-52506")
            .serial("12345")
            .firmware("1.0")
            .protocol(BootNotification.Protocols.IoT16)
            .build();
    // When & Then
    assertThat(calculator.heartbeatIntervalFor(boot)).isEqualTo(Duration.ofSeconds(600));
  }

  @Test
  void shouldReturn600sForDeviceALF9571445() {
    // Given
    BootNotification boot =
        BootNotification.builder()
            .deviceId("ALF-9571445")
            .vendor("Unknown Vendor")
            .model("Unknown Model")
            .serial("12345")
            .firmware("1.0")
            .protocol(BootNotification.Protocols.IoT16)
            .build();
    // When & Then
    assertThat(calculator.heartbeatIntervalFor(boot)).isEqualTo(Duration.ofSeconds(600));
  }

  @Test
  void shouldReturn2700sForDeviceT538264019() {
    // Given
    BootNotification boot =
        BootNotification.builder()
            .deviceId("t53_8264_019")
            .vendor("Unknown Vendor")
            .model("Unknown Model")
            .serial("12345")
            .firmware("1.0")
            .protocol(BootNotification.Protocols.IoT16)
            .build();
    // When & Then
    assertThat(calculator.heartbeatIntervalFor(boot)).isEqualTo(Duration.ofSeconds(2700));
  }

  @Test
  void shouldReturn2700sForDeviceEVBP15079256() {
    // Given
    BootNotification boot =
        BootNotification.builder()
            .deviceId("EVB-P15079256")
            .vendor("Unknown Vendor")
            .model("Unknown Model")
            .serial("12345")
            .firmware("1.0")
            .protocol(BootNotification.Protocols.IoT16)
            .build();
    // When & Then
    assertThat(calculator.heartbeatIntervalFor(boot)).isEqualTo(Duration.ofSeconds(2700));
  }

  @Test
  void shouldReturn60sForAlfenBVWithModelNG92052506() {
    // Given
    BootNotification boot =
        BootNotification.builder()
            .deviceId("UNKNOWN-DEVICE")
            .vendor("Alfen BV")
            .model("NG920-52506")
            .serial("12345")
            .firmware("1.0")
            .protocol(BootNotification.Protocols.IoT16)
            .build();
    // When & Then
    assertThat(calculator.heartbeatIntervalFor(boot)).isEqualTo(Duration.ofSeconds(60));
  }

  @Test
  void shouldReturn60sForAlfenBVWithModelNG92052507() {
    // Given
    BootNotification boot =
        BootNotification.builder()
            .deviceId("UNKNOWN-DEVICE")
            .vendor("Alfen BV")
            .model("NG920-52507")
            .serial("12345")
            .firmware("1.0")
            .protocol(BootNotification.Protocols.IoT16)
            .build();
    // When & Then
    assertThat(calculator.heartbeatIntervalFor(boot)).isEqualTo(Duration.ofSeconds(60));
  }

  @Test
  void shouldReturn60sForAlfenBVWithModelNG92052508() {
    // Given
    BootNotification boot =
        BootNotification.builder()
            .deviceId("UNKNOWN-DEVICE")
            .vendor("Alfen BV")
            .model("NG920-52508")
            .serial("12345")
            .firmware("1.0")
            .protocol(BootNotification.Protocols.IoT16)
            .build();
    // When & Then
    assertThat(calculator.heartbeatIntervalFor(boot)).isEqualTo(Duration.ofSeconds(60));
  }

  @Test
  void shouldReturn60sForAlfenBVWithModelNG92052509() {
    // Given
    BootNotification boot =
        BootNotification.builder()
            .deviceId("UNKNOWN-DEVICE")
            .vendor("Alfen BV")
            .model("NG920-52509")
            .serial("12345")
            .firmware("1.0")
            .protocol(BootNotification.Protocols.IoT16)
            .build();
    // When & Then
    assertThat(calculator.heartbeatIntervalFor(boot)).isEqualTo(Duration.ofSeconds(60));
  }

  @Test
  void shouldReturn1800sForAlfenBVWithModelNG92052505() {
    // Given
    BootNotification boot =
        BootNotification.builder()
            .deviceId("UNKNOWN-DEVICE")
            .vendor("Alfen BV")
            .model("NG920-52505")
            .serial("12345")
            .firmware("1.0")
            .protocol(BootNotification.Protocols.IoT16)
            .build();
    // When & Then
    assertThat(calculator.heartbeatIntervalFor(boot)).isEqualTo(Duration.ofSeconds(1800));
  }

  @Test
  void shouldReturn120sForChargeStormAB() {
    // Given
    BootNotification boot =
        BootNotification.builder()
            .deviceId("UNKNOWN-DEVICE")
            .vendor("ChargeStorm AB")
            .model("Chargestorm Connected")
            .serial("12345")
            .firmware("1.0")
            .protocol(BootNotification.Protocols.IoT16)
            .build();
    // When & Then
    assertThat(calculator.heartbeatIntervalFor(boot)).isEqualTo(Duration.ofSeconds(120));
  }

  @Test
  void shouldReturn1800sForUnknownDevice() {
    // Given
    BootNotification boot =
        BootNotification.builder()
            .deviceId("UNKNOWN-DEVICE")
            .vendor("Unknown Vendor")
            .model("Unknown Model")
            .serial("12345")
            .firmware("1.0")
            .protocol(BootNotification.Protocols.IoT16)
            .build();
    // When & Then
    assertThat(calculator.heartbeatIntervalFor(boot)).isEqualTo(Duration.ofSeconds(1800));
  }

  @Test
  void shouldPrioritizeDeviceIdRuleOverModelRule() {
    // Given
    BootNotification boot =
        BootNotification.builder()
            .deviceId("EVB-P4562137")
            .vendor("Alfen BV")
            .model("NG920-52507")
            .serial("12345")
            .firmware("1.0")
            .protocol(BootNotification.Protocols.IoT16)
            .build();
    // When & Then
    assertThat(calculator.heartbeatIntervalFor(boot)).isEqualTo(Duration.ofSeconds(600));
  }
}
