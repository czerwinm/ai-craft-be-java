package devices.configuration.communication;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class IntervalConfigurationTest {

    @Test
    void shouldReturnIntervalForSpecificDeviceId() {
        // Given
        IntervalConfiguration config = IntervalConfiguration.create(
                List.of(
                        new IntervalRule.DeviceIdRule(Duration.ofSeconds(600), Set.of("EVB-P4562137", "ALF-9571445")),
                        new IntervalRule.DeviceIdRule(Duration.ofSeconds(2700), Set.of("t53_8264_019", "EVB-P15079256"))
                ),
                List.of(
                        new IntervalRule.ModelRule(Duration.ofSeconds(60), "Alfen BV", "NG920-5250[6-9]"),
                        new IntervalRule.ModelRule(Duration.ofSeconds(120), "ChargeStorm AB", "Chargestorm Connected")
                ),
                new IntervalRule.DefaultRule(Duration.ofSeconds(1800))
        );
        BootNotification boot = BootNotification.builder()
                .deviceId("EVB-P4562137")
                .vendor("Alfen BV")
                .model("NG920-52506")
                .serial("12345")
                .firmware("1.0")
                .protocol(BootNotification.Protocols.IoT16)
                .build();
        // When
        Duration interval = config.calculateInterval(boot);
        // Then
        assertThat(interval).isEqualTo(Duration.ofSeconds(600));
    }

    @Test
    void shouldReturnIntervalForAnotherSpecificDeviceId() {
        // Given
        IntervalConfiguration config = IntervalConfiguration.create(
                List.of(
                        new IntervalRule.DeviceIdRule(Duration.ofSeconds(600), Set.of("EVB-P4562137", "ALF-9571445")),
                        new IntervalRule.DeviceIdRule(Duration.ofSeconds(2700), Set.of("t53_8264_019", "EVB-P15079256"))
                ),
                List.of(
                        new IntervalRule.ModelRule(Duration.ofSeconds(60), "Alfen BV", "NG920-5250[6-9]"),
                        new IntervalRule.ModelRule(Duration.ofSeconds(120), "ChargeStorm AB", "Chargestorm Connected")
                ),
                new IntervalRule.DefaultRule(Duration.ofSeconds(1800))
        );
        BootNotification boot = BootNotification.builder()
                .deviceId("t53_8264_019")
                .vendor("Some Vendor")
                .model("Some Model")
                .serial("12345")
                .firmware("1.0")
                .protocol(BootNotification.Protocols.IoT16)
                .build();
        // When
        Duration interval = config.calculateInterval(boot);
        // Then
        assertThat(interval).isEqualTo(Duration.ofSeconds(2700));
    }

    @Test
    void shouldReturnIntervalForModelRuleWhenDeviceIdNotMatched() {
        // Given
        IntervalConfiguration config = IntervalConfiguration.create(
                List.of(
                        new IntervalRule.DeviceIdRule(Duration.ofSeconds(600), Set.of("EVB-P4562137", "ALF-9571445")),
                        new IntervalRule.DeviceIdRule(Duration.ofSeconds(2700), Set.of("t53_8264_019", "EVB-P15079256"))
                ),
                List.of(
                        new IntervalRule.ModelRule(Duration.ofSeconds(60), "Alfen BV", "NG920-5250[6-9]"),
                        new IntervalRule.ModelRule(Duration.ofSeconds(120), "ChargeStorm AB", "Chargestorm Connected")
                ),
                new IntervalRule.DefaultRule(Duration.ofSeconds(1800))
        );
        BootNotification boot = BootNotification.builder()
                .deviceId("UNKNOWN-DEVICE")
                .vendor("Alfen BV")
                .model("NG920-52507")
                .serial("12345")
                .firmware("1.0")
                .protocol(BootNotification.Protocols.IoT16)
                .build();
        // When
        Duration interval = config.calculateInterval(boot);
        // Then
        assertThat(interval).isEqualTo(Duration.ofSeconds(60));
    }

    @Test
    void shouldReturnIntervalForChargeStormModel() {
        // Given
        IntervalConfiguration config = IntervalConfiguration.create(
                List.of(
                        new IntervalRule.DeviceIdRule(Duration.ofSeconds(600), Set.of("EVB-P4562137", "ALF-9571445")),
                        new IntervalRule.DeviceIdRule(Duration.ofSeconds(2700), Set.of("t53_8264_019", "EVB-P15079256"))
                ),
                List.of(
                        new IntervalRule.ModelRule(Duration.ofSeconds(60), "Alfen BV", "NG920-5250[6-9]"),
                        new IntervalRule.ModelRule(Duration.ofSeconds(120), "ChargeStorm AB", "Chargestorm Connected")
                ),
                new IntervalRule.DefaultRule(Duration.ofSeconds(1800))
        );
        BootNotification boot = BootNotification.builder()
                .deviceId("CS-DEVICE")
                .vendor("ChargeStorm AB")
                .model("Chargestorm Connected")
                .serial("12345")
                .firmware("1.0")
                .protocol(BootNotification.Protocols.IoT16)
                .build();
        // When
        Duration interval = config.calculateInterval(boot);
        // Then
        assertThat(interval).isEqualTo(Duration.ofSeconds(120));
    }

    @Test
    void shouldReturnDefaultIntervalWhenNoRulesMatch() {
        // Given
        IntervalConfiguration config = IntervalConfiguration.create(
                List.of(
                        new IntervalRule.DeviceIdRule(Duration.ofSeconds(600), Set.of("EVB-P4562137", "ALF-9571445")),
                        new IntervalRule.DeviceIdRule(Duration.ofSeconds(2700), Set.of("t53_8264_019", "EVB-P15079256"))
                ),
                List.of(
                        new IntervalRule.ModelRule(Duration.ofSeconds(60), "Alfen BV", "NG920-5250[6-9]"),
                        new IntervalRule.ModelRule(Duration.ofSeconds(120), "ChargeStorm AB", "Chargestorm Connected")
                ),
                new IntervalRule.DefaultRule(Duration.ofSeconds(1800))
        );
        BootNotification boot = BootNotification.builder()
                .deviceId("UNKNOWN-DEVICE")
                .vendor("Unknown Vendor")
                .model("Unknown Model")
                .serial("12345")
                .firmware("1.0")
                .protocol(BootNotification.Protocols.IoT16)
                .build();
        // When
        Duration interval = config.calculateInterval(boot);
        // Then
        assertThat(interval).isEqualTo(Duration.ofSeconds(1800));
    }

    @Test
    void shouldPrioritizeDeviceIdRulesOverModelRules() {
        // Given
        IntervalConfiguration config = IntervalConfiguration.create(
                List.of(
                        new IntervalRule.DeviceIdRule(Duration.ofSeconds(600), Set.of("EVB-P4562137"))
                ),
                List.of(
                        new IntervalRule.ModelRule(Duration.ofSeconds(60), "Alfen BV", "NG920-5250[6-9]")
                ),
                new IntervalRule.DefaultRule(Duration.ofSeconds(1800))
        );
        BootNotification boot = BootNotification.builder()
                .deviceId("EVB-P4562137")
                .vendor("Alfen BV")
                .model("NG920-52507")
                .serial("12345")
                .firmware("1.0")
                .protocol(BootNotification.Protocols.IoT16)
                .build();
        // When
        Duration interval = config.calculateInterval(boot);
        // Then
        assertThat(interval).isEqualTo(Duration.ofSeconds(600));
    }

    @Test
    void shouldReturnFirstMatchingDeviceIdRule() {
        // Given
        IntervalConfiguration config = IntervalConfiguration.create(
                List.of(
                        new IntervalRule.DeviceIdRule(Duration.ofSeconds(600), Set.of("EVB-P4562137", "ALF-9571445")),
                        new IntervalRule.DeviceIdRule(Duration.ofSeconds(2700), Set.of("EVB-P4562137", "OTHER"))
                ),
                List.of(),
                new IntervalRule.DefaultRule(Duration.ofSeconds(1800))
        );
        BootNotification boot = BootNotification.builder()
                .deviceId("EVB-P4562137")
                .vendor("Vendor")
                .model("Model")
                .serial("12345")
                .firmware("1.0")
                .protocol(BootNotification.Protocols.IoT16)
                .build();
        // When
        Duration interval = config.calculateInterval(boot);
        // Then
        assertThat(interval).isEqualTo(Duration.ofSeconds(600));
    }

    @Test
    void shouldReturnFirstMatchingModelRule() {
        // Given
        IntervalConfiguration config = IntervalConfiguration.create(
                List.of(),
                List.of(
                        new IntervalRule.ModelRule(Duration.ofSeconds(60), "Alfen BV", "NG920-.*"),
                        new IntervalRule.ModelRule(Duration.ofSeconds(120), "Alfen BV", "NG920-5250[6-9]")
                ),
                new IntervalRule.DefaultRule(Duration.ofSeconds(1800))
        );
        BootNotification boot = BootNotification.builder()
                .deviceId("DEVICE")
                .vendor("Alfen BV")
                .model("NG920-52507")
                .serial("12345")
                .firmware("1.0")
                .protocol(BootNotification.Protocols.IoT16)
                .build();
        // When
        Duration interval = config.calculateInterval(boot);
        // Then
        assertThat(interval).isEqualTo(Duration.ofSeconds(60));
    }
}
