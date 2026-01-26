package devices.configuration.communication;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IntervalRuleTest {

    @Test
    void deviceIdRuleShouldMatchSpecificDeviceId() {
        // Given
        IntervalRule.DeviceIdRule rule = new IntervalRule.DeviceIdRule(
                Duration.ofSeconds(600),
                Set.of("EVB-P4562137", "ALF-9571445")
        );
        BootNotification boot = BootNotification.builder()
                .deviceId("EVB-P4562137")
                .vendor("Alfen BV")
                .model("NG920-52506")
                .serial("12345")
                .firmware("1.0")
                .protocol(BootNotification.Protocols.IoT16)
                .build();
        // When & Then
        assertThat(rule.matches(boot)).isTrue();
        assertThat(rule.interval()).isEqualTo(Duration.ofSeconds(600));
    }

    @Test
    void deviceIdRuleShouldNotMatchDifferentDeviceId() {
        // Given
        IntervalRule.DeviceIdRule rule = new IntervalRule.DeviceIdRule(
                Duration.ofSeconds(600),
                Set.of("EVB-P4562137", "ALF-9571445")
        );
        BootNotification boot = BootNotification.builder()
                .deviceId("OTHER-DEVICE")
                .vendor("Alfen BV")
                .model("NG920-52506")
                .serial("12345")
                .firmware("1.0")
                .protocol(BootNotification.Protocols.IoT16)
                .build();
        // When & Then
        assertThat(rule.matches(boot)).isFalse();
    }

    @Test
    void deviceIdRuleShouldRejectNullInterval() {
        // When & Then
        assertThatThrownBy(() -> new IntervalRule.DeviceIdRule(
                null,
                Set.of("EVB-P4562137")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Interval must be positive");
    }

    @Test
    void deviceIdRuleShouldRejectNegativeInterval() {
        // When & Then
        assertThatThrownBy(() -> new IntervalRule.DeviceIdRule(
                Duration.ofSeconds(-1),
                Set.of("EVB-P4562137")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Interval must be positive");
    }

    @Test
    void deviceIdRuleShouldRejectEmptyDeviceIds() {
        // When & Then
        assertThatThrownBy(() -> new IntervalRule.DeviceIdRule(
                Duration.ofSeconds(600),
                Set.of()
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Device IDs cannot be empty");
    }

    @Test
    void modelRuleShouldMatchVendorAndModelRegex() {
        // Given
        IntervalRule.ModelRule rule = new IntervalRule.ModelRule(
                Duration.ofSeconds(60),
                "Alfen BV",
                "NG920-5250[6-9]"
        );
        BootNotification boot = BootNotification.builder()
                .deviceId("EVB-P4562137")
                .vendor("Alfen BV")
                .model("NG920-52506")
                .serial("12345")
                .firmware("1.0")
                .protocol(BootNotification.Protocols.IoT16)
                .build();
        // When & Then
        assertThat(rule.matches(boot)).isTrue();
        assertThat(rule.interval()).isEqualTo(Duration.ofSeconds(60));
    }

    @Test
    void modelRuleShouldMatchDifferentModelInRange() {
        // Given
        IntervalRule.ModelRule rule = new IntervalRule.ModelRule(
                Duration.ofSeconds(60),
                "Alfen BV",
                "NG920-5250[6-9]"
        );
        BootNotification boot = BootNotification.builder()
                .deviceId("OTHER-DEVICE")
                .vendor("Alfen BV")
                .model("NG920-52509")
                .serial("12345")
                .firmware("1.0")
                .protocol(BootNotification.Protocols.IoT16)
                .build();
        // When & Then
        assertThat(rule.matches(boot)).isTrue();
    }

    @Test
    void modelRuleShouldNotMatchModelOutsideRange() {
        // Given
        IntervalRule.ModelRule rule = new IntervalRule.ModelRule(
                Duration.ofSeconds(60),
                "Alfen BV",
                "NG920-5250[6-9]"
        );
        BootNotification boot = BootNotification.builder()
                .deviceId("OTHER-DEVICE")
                .vendor("Alfen BV")
                .model("NG920-52505")
                .serial("12345")
                .firmware("1.0")
                .protocol(BootNotification.Protocols.IoT16)
                .build();
        // When & Then
        assertThat(rule.matches(boot)).isFalse();
    }

    @Test
    void modelRuleShouldNotMatchDifferentVendor() {
        // Given
        IntervalRule.ModelRule rule = new IntervalRule.ModelRule(
                Duration.ofSeconds(60),
                "Alfen BV",
                "NG920-5250[6-9]"
        );
        BootNotification boot = BootNotification.builder()
                .deviceId("OTHER-DEVICE")
                .vendor("ChargeStorm AB")
                .model("NG920-52506")
                .serial("12345")
                .firmware("1.0")
                .protocol(BootNotification.Protocols.IoT16)
                .build();
        // When & Then
        assertThat(rule.matches(boot)).isFalse();
    }

    @Test
    void modelRuleShouldMatchSimpleModel() {
        // Given
        IntervalRule.ModelRule rule = new IntervalRule.ModelRule(
                Duration.ofSeconds(120),
                "ChargeStorm AB",
                "Chargestorm Connected"
        );
        BootNotification boot = BootNotification.builder()
                .deviceId("CS-12345")
                .vendor("ChargeStorm AB")
                .model("Chargestorm Connected")
                .serial("12345")
                .firmware("1.0")
                .protocol(BootNotification.Protocols.IoT16)
                .build();
        // When & Then
        assertThat(rule.matches(boot)).isTrue();
        assertThat(rule.interval()).isEqualTo(Duration.ofSeconds(120));
    }

    @Test
    void modelRuleShouldRejectNullInterval() {
        // When & Then
        assertThatThrownBy(() -> new IntervalRule.ModelRule(
                null,
                "Alfen BV",
                "NG920-5250[6-9]"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Interval must be positive");
    }

    @Test
    void modelRuleShouldRejectBlankVendor() {
        // When & Then
        assertThatThrownBy(() -> new IntervalRule.ModelRule(
                Duration.ofSeconds(60),
                "",
                "NG920-5250[6-9]"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Vendor cannot be empty");
    }

    @Test
    void modelRuleShouldRejectBlankModelRegex() {
        // When & Then
        assertThatThrownBy(() -> new IntervalRule.ModelRule(
                Duration.ofSeconds(60),
                "Alfen BV",
                ""
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Model regex cannot be empty");
    }

    @Test
    void defaultRuleShouldAlwaysMatch() {
        // Given
        IntervalRule.DefaultRule rule = new IntervalRule.DefaultRule(Duration.ofSeconds(1800));
        BootNotification boot = BootNotification.builder()
                .deviceId("ANY-DEVICE")
                .vendor("Any Vendor")
                .model("Any Model")
                .serial("12345")
                .firmware("1.0")
                .protocol(BootNotification.Protocols.IoT16)
                .build();
        // When & Then
        assertThat(rule.matches(boot)).isTrue();
        assertThat(rule.interval()).isEqualTo(Duration.ofSeconds(1800));
    }

    @Test
    void defaultRuleShouldRejectNullInterval() {
        // When & Then
        assertThatThrownBy(() -> new IntervalRule.DefaultRule(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Interval must be positive");
    }
}
