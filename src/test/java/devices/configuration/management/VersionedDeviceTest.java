package devices.configuration.management;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VersionedDeviceTest {

    @Test
    void shouldCreateVersionedDevice() {
        // Given
        DeviceConfiguration device = DeviceConfiguration.newDevice("device-1");
        
        // When
        VersionedDevice versioned = new VersionedDevice(device, 1);
        
        // Then
        assertThat(versioned.device()).isEqualTo(device);
        assertThat(versioned.version()).isEqualTo(1);
    }

    @Test
    void shouldAcceptZeroVersion() {
        // Given
        DeviceConfiguration device = DeviceConfiguration.newDevice("device-1");
        
        // When
        VersionedDevice versioned = new VersionedDevice(device, 0);
        
        // Then
        assertThat(versioned.version()).isEqualTo(0);
    }

    @Test
    void shouldRejectNullDevice() {
        // When & Then
        assertThatThrownBy(() -> new VersionedDevice(null, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Device cannot be null");
    }

    @Test
    void shouldRejectNegativeVersion() {
        // Given
        DeviceConfiguration device = DeviceConfiguration.newDevice("device-1");
        
        // When & Then
        assertThatThrownBy(() -> new VersionedDevice(device, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Version must be non-negative");
    }
}
