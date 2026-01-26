package devices.configuration.management;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceConfigurationServiceTest {

    @Mock
    private DeviceConfigurationRepository repository;

    @InjectMocks
    private DeviceConfigurationService service;

    @Test
    void shouldCreateNewDevice() {
        // Given
        when(repository.exists("device-1")).thenReturn(false);
        when(repository.save(any(DeviceConfiguration.class), isNull())).thenReturn(1L);

        // When
        DeviceConfigurationSnapshot snapshot = service.createDevice("device-1");

        // Then
        assertThat(snapshot.deviceId()).isEqualTo("device-1");
        verify(repository).exists("device-1");
        verify(repository).save(any(DeviceConfiguration.class), isNull());
    }

    @Test
    void shouldThrowExceptionWhenDeviceAlreadyExists() {
        // Given
        when(repository.exists("device-1")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> service.createDevice("device-1"))
                .isInstanceOf(DeviceAlreadyExistsException.class)
                .hasMessageContaining("device-1");
    }

    @Test
    void shouldGetDeviceById() {
        // Given
        DeviceConfiguration device = DeviceConfiguration.newDevice("device-1");
        VersionedDevice versioned = new VersionedDevice(device, 1);
        when(repository.findById("device-1")).thenReturn(Optional.of(versioned));

        // When
        Optional<DeviceConfigurationSnapshot> result = service.getDevice("device-1");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().deviceId()).isEqualTo("device-1");
    }

    @Test
    void shouldReturnEmptyWhenDeviceNotFound() {
        // Given
        when(repository.findById("device-1")).thenReturn(Optional.empty());

        // When
        Optional<DeviceConfigurationSnapshot> result = service.getDevice("device-1");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldUpdateOwnership() {
        // Given
        DeviceConfiguration device = DeviceConfiguration.newDevice("device-1");
        VersionedDevice versioned = new VersionedDevice(device, 1);
        when(repository.findById("device-1")).thenReturn(Optional.of(versioned));
        when(repository.save(any(DeviceConfiguration.class), eq(1L))).thenReturn(2L);
        Ownership newOwnership = Ownership.of("operator1", "provider1");

        // When
        DeviceConfigurationSnapshot snapshot = service.updateOwnership("device-1", newOwnership, 1);

        // Then
        assertThat(snapshot.ownership()).isEqualTo(newOwnership);
        verify(repository).save(any(DeviceConfiguration.class), eq(1L));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentDevice() {
        // Given
        when(repository.findById("device-1")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> service.updateOwnership("device-1", Ownership.unowned(), 1))
                .isInstanceOf(DeviceNotFoundException.class)
                .hasMessageContaining("device-1");
    }

    @Test
    void shouldUpdateLocation() {
        // Given
        DeviceConfiguration device = DeviceConfiguration.newDevice("device-1");
        VersionedDevice versioned = new VersionedDevice(device, 1);
        when(repository.findById("device-1")).thenReturn(Optional.of(versioned));
        when(repository.save(any(DeviceConfiguration.class), eq(1L))).thenReturn(2L);
        Location location = Location.of("Street", "1", "City", "00-000", "State", "Country",
                new Coordinates(52.0, 21.0));

        // When
        DeviceConfigurationSnapshot snapshot = service.updateLocation("device-1", location, 1);

        // Then
        assertThat(snapshot.location()).isEqualTo(location);
    }

    @Test
    void shouldUpdateOpeningHours() {
        // Given
        DeviceConfiguration device = DeviceConfiguration.newDevice("device-1");
        VersionedDevice versioned = new VersionedDevice(device, 1);
        when(repository.findById("device-1")).thenReturn(Optional.of(versioned));
        when(repository.save(any(DeviceConfiguration.class), eq(1L))).thenReturn(2L);
        OpeningHours openingHours = OpeningHours.of(false);

        // When
        DeviceConfigurationSnapshot snapshot = service.updateOpeningHours("device-1", openingHours, 1);

        // Then
        assertThat(snapshot.openingHours()).isEqualTo(openingHours);
    }

    @Test
    void shouldUpdateSettings() {
        // Given
        DeviceConfiguration device = DeviceConfiguration.newDevice("device-1");
        VersionedDevice versioned = new VersionedDevice(device, 1);
        when(repository.findById("device-1")).thenReturn(Optional.of(versioned));
        when(repository.save(any(DeviceConfiguration.class), eq(1L))).thenReturn(2L);
        Settings settings = Settings.of(true, true, true, true, true, true);

        // When
        DeviceConfigurationSnapshot snapshot = service.updateSettings("device-1", settings, 1);

        // Then
        assertThat(snapshot.settings()).isEqualTo(settings);
    }

    @Test
    void shouldDeleteDevice() {
        // When
        service.deleteDevice("device-1", 1);

        // Then
        verify(repository).delete("device-1", 1);
    }

    @Test
    void shouldListDevicesWithPagination() {
        // Given
        DeviceConfiguration device1 = DeviceConfiguration.newDevice("device-1");
        DeviceConfiguration device2 = DeviceConfiguration.newDevice("device-2");
        VersionedDevice versioned1 = new VersionedDevice(device1, 1);
        VersionedDevice versioned2 = new VersionedDevice(device2, 1);
        Pageable pageable = PageRequest.of(0, 10);
        Page<VersionedDevice> page = new PageImpl<>(List.of(versioned1, versioned2), pageable, 2);
        when(repository.findAll(pageable)).thenReturn(page);

        // When
        Page<DeviceConfigurationSnapshot> result = service.listDevices(pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).deviceId()).isEqualTo("device-1");
        assertThat(result.getContent().get(1).deviceId()).isEqualTo("device-2");
        assertThat(result.getTotalElements()).isEqualTo(2);
    }
}
