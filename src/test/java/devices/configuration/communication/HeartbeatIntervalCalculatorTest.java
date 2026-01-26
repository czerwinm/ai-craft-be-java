package devices.configuration.communication;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class HeartbeatIntervalCalculatorTest {

  @Test
  void shouldCalculateIntervalBasedOnDeviceId() {
    // Given
    IntervalConfiguration config =
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
    HeartbeatIntervalCalculator calculator = new HeartbeatIntervalCalculator(config);
    BootNotification boot =
        BootNotification.builder()
            .deviceId("ALF-9571445")
            .vendor("Some Vendor")
            .model("Some Model")
            .serial("12345")
            .firmware("1.0")
            .protocol(BootNotification.Protocols.IoT16)
            .build();
    // When
    Duration interval = calculator.heartbeatIntervalFor(boot);
    // Then
    assertThat(interval).isEqualTo(Duration.ofSeconds(600));
  }

  @Test
  void shouldCalculateIntervalBasedOnModel() {
    // Given
    IntervalConfiguration config =
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
    HeartbeatIntervalCalculator calculator = new HeartbeatIntervalCalculator(config);
    BootNotification boot =
        BootNotification.builder()
            .deviceId("UNKNOWN-DEVICE")
            .vendor("ChargeStorm AB")
            .model("Chargestorm Connected")
            .serial("12345")
            .firmware("1.0")
            .protocol(BootNotification.Protocols.IoT16)
            .build();
    // When
    Duration interval = calculator.heartbeatIntervalFor(boot);
    // Then
    assertThat(interval).isEqualTo(Duration.ofSeconds(120));
  }

  @Test
  void shouldCalculateDefaultInterval() {
    // Given
    IntervalConfiguration config =
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
    HeartbeatIntervalCalculator calculator = new HeartbeatIntervalCalculator(config);
    BootNotification boot =
        BootNotification.builder()
            .deviceId("UNKNOWN-DEVICE")
            .vendor("Unknown Vendor")
            .model("Unknown Model")
            .serial("12345")
            .firmware("1.0")
            .protocol(BootNotification.Protocols.IoT16)
            .build();
    // When
    Duration interval = calculator.heartbeatIntervalFor(boot);
    // Then
    assertThat(interval).isEqualTo(Duration.ofSeconds(1800));
  }
}
