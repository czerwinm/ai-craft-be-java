package devices.configuration.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import devices.configuration.IntegrationTest;
import devices.configuration.IntegrationTestConfiguration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest(profiles = {"integration-test"})
@Import(IntegrationTestConfiguration.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@RecordApplicationEvents
class DeviceConfigurationDocumentRepositoryTest {

  @Autowired private DeviceConfigurationRepository repository;

  @Autowired
  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
  private ApplicationEvents events;

  @Test
  void shouldSaveAndRetrieveDevice() {
    // Given
    DeviceConfiguration device = DeviceConfiguration.newDevice("test-device-1");
    device.changeOwnership(Ownership.of("operator1", "provider1"));

    // When
    long version = repository.save(device, null);
    Optional<VersionedDevice> retrieved = repository.findById("test-device-1");

    // Then
    assertThat(retrieved).isPresent();
    assertThat(retrieved.get().device().toSnapshot().deviceId()).isEqualTo("test-device-1");
    assertThat(retrieved.get().device().toSnapshot().ownership())
        .isEqualTo(Ownership.of("operator1", "provider1"));
    assertThat(retrieved.get().version()).isEqualTo(version);
  }

  @Test
  void shouldReturnEmptyForNonExistentDevice() {
    // When
    Optional<VersionedDevice> result = repository.findById("non-existent-id");

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  void shouldCheckIfDeviceExists() {
    // Given
    DeviceConfiguration device = DeviceConfiguration.newDevice("test-device-2");
    repository.save(device, null);

    // When
    boolean exists = repository.exists("test-device-2");
    boolean notExists = repository.exists("non-existent");

    // Then
    assertThat(exists).isTrue();
    assertThat(notExists).isFalse();
  }

  @Test
  void shouldUpdateDeviceWithOptimisticLocking() {
    // Given
    DeviceConfiguration device = DeviceConfiguration.newDevice("test-device-3");
    long v1 = repository.save(device, null);

    VersionedDevice versioned = repository.findById("test-device-3").orElseThrow();
    DeviceConfiguration retrieved = versioned.device();
    retrieved.changeOwnership(Ownership.of("operator2", "provider2"));

    // When
    long v2 = repository.save(retrieved, v1);

    // Then
    assertThat(v2).isGreaterThan(v1);
    VersionedDevice updated = repository.findById("test-device-3").orElseThrow();
    assertThat(updated.version()).isEqualTo(v2);
    assertThat(updated.device().toSnapshot().ownership())
        .isEqualTo(Ownership.of("operator2", "provider2"));
  }

  @Test
  void shouldThrowOptimisticLockExceptionOnVersionMismatch() {
    // Given
    DeviceConfiguration device = DeviceConfiguration.newDevice("test-device-4");
    long v1 = repository.save(device, null);

    VersionedDevice versioned = repository.findById("test-device-4").orElseThrow();
    DeviceConfiguration retrieved = versioned.device();
    retrieved.changeOwnership(Ownership.of("operator3", "provider3"));

    // When & Then
    assertThatThrownBy(() -> repository.save(retrieved, v1 + 999))
        .isInstanceOf(OptimisticLockException.class)
        .hasMessageContaining("test-device-4");
  }

  @Test
  void shouldDeleteDevice() {
    // Given
    DeviceConfiguration device = DeviceConfiguration.newDevice("test-device-5");
    long version = repository.save(device, null);

    // When
    repository.delete("test-device-5", version);

    // Then
    assertThat(repository.findById("test-device-5")).isEmpty();
  }

  @Test
  void shouldThrowExceptionWhenDeletingNonExistentDevice() {
    // When & Then
    assertThatThrownBy(() -> repository.delete("non-existent", 1))
        .isInstanceOf(DeviceNotFoundException.class)
        .hasMessageContaining("non-existent");
  }

  @Test
  void shouldThrowOptimisticLockExceptionWhenDeletingWithWrongVersion() {
    // Given
    DeviceConfiguration device = DeviceConfiguration.newDevice("test-device-6");
    long version = repository.save(device, null);

    // When & Then
    assertThatThrownBy(() -> repository.delete("test-device-6", version + 1))
        .isInstanceOf(OptimisticLockException.class)
        .hasMessageContaining("test-device-6");
  }

  @Test
  void shouldListDevicesWithPagination() {
    // Given
    DeviceConfiguration device1 = DeviceConfiguration.newDevice("test-device-7");
    DeviceConfiguration device2 = DeviceConfiguration.newDevice("test-device-8");
    DeviceConfiguration device3 = DeviceConfiguration.newDevice("test-device-9");
    repository.save(device1, null);
    repository.save(device2, null);
    repository.save(device3, null);

    // When
    Pageable pageable = PageRequest.of(0, 2);
    Page<VersionedDevice> page = repository.findAll(pageable);

    // Then
    assertThat(page.getContent().size()).isLessThanOrEqualTo(2);
    assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(3);
  }

  @Test
  void shouldEmitDomainEvents() {
    // Given
    DeviceConfiguration device = DeviceConfiguration.newDevice("test-device-10");
    device.changeOwnership(Ownership.of("operator4", "provider4"));
    device.changeLocation(
        Location.of(
            "Street", "1", "City", "00-000", "State", "Country", new Coordinates(52.0, 21.0)));

    // When
    repository.save(device, null);

    // Then
    assertThat(events.stream(DomainEvent.class).toList()).hasSizeGreaterThanOrEqualTo(2);
    assertThat(events.stream(DeviceConfigurationSnapshot.class).toList())
        .hasSizeGreaterThanOrEqualTo(1);
  }
}
