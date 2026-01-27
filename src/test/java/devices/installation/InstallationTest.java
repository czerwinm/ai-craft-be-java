package devices.installation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import devices.configuration.management.Coordinates;
import devices.configuration.management.Ownership;
import org.junit.jupiter.api.Test;

class InstallationTest {

  @Test
  void shouldCreateInstallationFromWorkOrder() {
    // Given
    WorkOrderId workOrderId = WorkOrderId.of("WO-12345");
    InstallationContext context = InstallationContext.of("corporate", "POL", "wallbox", null);
    Ownership ownership = Ownership.of("Devicex.nl", "public-devices");

    // When
    Installation installation = Installation.fromWorkOrder(workOrderId, context, ownership);

    // Then
    InstallationSnapshot snapshot = installation.toSnapshot();
    assertThat(snapshot.workOrderId()).isEqualTo(workOrderId);
    assertThat(snapshot.phase()).isEqualTo(InstallationPhase.INITIAL);
    assertThat(snapshot.ownership()).isEqualTo(ownership);
    assertThat(snapshot.context()).isEqualTo(context);
    assertThat(snapshot.assignedInstallerId()).isNull();
    assertThat(snapshot.deviceIdentifier()).isNull();
    assertThat(snapshot.canFinish()).isFalse();

    assertThat(installation.events).hasSize(1);
    assertThat(installation.events.get(0)).isInstanceOf(InstallationEvent.WorkOrderReceived.class);
  }

  @Test
  void shouldAssignInstaller() {
    // Given
    Installation installation = createInstallation();
    installation.events.clear();
    InstallerId installer = InstallerId.of("installer-123");

    // When
    installation.assignInstaller(installer, false);

    // Then
    assertThat(installation.toSnapshot().assignedInstallerId()).isEqualTo(installer);
    assertThat(installation.events).hasSize(1);
    assertThat(installation.events.get(0)).isInstanceOf(InstallationEvent.InstallerAssigned.class);
  }

  @Test
  void shouldFailToAssignInstallerWhenAlreadyAssignedWithoutForce() {
    // Given
    Installation installation = createInstallation();
    installation.assignInstaller(InstallerId.of("installer-1"), false);

    // When & Then
    assertThatThrownBy(() -> installation.assignInstaller(InstallerId.of("installer-2"), false))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("already assigned");
  }

  @Test
  void shouldForceReassignInstaller() {
    // Given
    Installation installation = createInstallation();
    installation.assignInstaller(InstallerId.of("installer-1"), false);
    installation.events.clear();

    // When
    InstallerId newInstaller = InstallerId.of("installer-2");
    installation.assignInstaller(newInstaller, true);

    // Then
    assertThat(installation.toSnapshot().assignedInstallerId()).isEqualTo(newInstaller);
    assertThat(installation.events).hasSize(1);
    assertThat(installation.events.get(0))
        .isInstanceOf(InstallationEvent.InstallerReassigned.class);
    InstallationEvent.InstallerReassigned event =
        (InstallationEvent.InstallerReassigned) installation.events.get(0);
    assertThat(event.forced()).isTrue();
  }

  @Test
  void shouldStartInstallation() {
    // Given
    Installation installation = createInstallation();
    installation.assignInstaller(InstallerId.of("installer-1"), false);
    installation.events.clear();

    // When
    installation.start();

    // Then
    assertThat(installation.events).hasSize(1);
    assertThat(installation.events.get(0))
        .isInstanceOf(InstallationEvent.InstallationStarted.class);
  }

  @Test
  void shouldFailToStartWithoutAssignedInstaller() {
    // Given
    Installation installation = createInstallation();

    // When & Then
    assertThatThrownBy(() -> installation.start())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("without assigned installer");
  }

  @Test
  void shouldAssignDevice() {
    // Given
    Installation installation = createInstallation();
    installation.events.clear();
    DeviceIdentifier device = DeviceIdentifier.of("DEV-12345");

    // When
    installation.assignDevice(device);

    // Then
    InstallationSnapshot snapshot = installation.toSnapshot();
    assertThat(snapshot.deviceIdentifier()).isEqualTo(device);
    assertThat(snapshot.phase()).isEqualTo(InstallationPhase.DEVICE_ASSIGNED);
    assertThat(installation.events).hasSize(1);
    assertThat(installation.events.get(0)).isInstanceOf(InstallationEvent.DeviceAssigned.class);
  }

  @Test
  void shouldSwapDeviceAndInvalidateBootConfirmation() {
    // Given
    Installation installation = createInstallation();
    DeviceIdentifier oldDevice = DeviceIdentifier.of("DEV-OLD");
    installation.assignDevice(oldDevice);
    BootDetails boot = BootDetails.of("Garo", "CPF25", "1.0", "S123", "IoT16");
    installation.registerBootNotification(boot);
    installation.confirmBoot();
    installation.events.clear();

    // When
    DeviceIdentifier newDevice = DeviceIdentifier.of("DEV-NEW");
    installation.assignDevice(newDevice);

    // Then
    InstallationSnapshot snapshot = installation.toSnapshot();
    assertThat(snapshot.deviceIdentifier()).isEqualTo(newDevice);
    assertThat(snapshot.phase()).isEqualTo(InstallationPhase.DEVICE_ASSIGNED);
    assertThat(snapshot.bootConfirmed()).isFalse();
    assertThat(snapshot.lastRelevantBoot()).isNull();
    assertThat(installation.events).hasSize(1);
    assertThat(installation.events.get(0)).isInstanceOf(InstallationEvent.DeviceSwapped.class);
  }

  @Test
  void shouldRegisterBootNotificationAndRequireConfirmation() {
    // Given
    Installation installation = createInstallation();
    installation.assignDevice(DeviceIdentifier.of("DEV-12345"));
    installation.events.clear();
    BootDetails boot = BootDetails.of("Garo", "CPF25", "1.0", "S123", "IoT16");

    // When
    installation.registerBootNotification(boot);

    // Then
    InstallationSnapshot snapshot = installation.toSnapshot();
    assertThat(snapshot.lastRelevantBoot()).isEqualTo(boot);
    assertThat(snapshot.bootConfirmed()).isFalse();
    assertThat(snapshot.phase()).isEqualTo(InstallationPhase.BOOT_CONFIRMATION_REQUIRED);
    assertThat(installation.events).hasSize(1);
    assertThat(installation.events.get(0))
        .isInstanceOf(InstallationEvent.BootNotificationReceived.class);
  }

  @Test
  void shouldRequireReconfirmationWhenFirmwareChanges() {
    // Given
    Installation installation = createInstallation();
    installation.assignDevice(DeviceIdentifier.of("DEV-12345"));
    BootDetails boot1 = BootDetails.of("Garo", "CPF25", "1.0", "S123", "IoT16");
    installation.registerBootNotification(boot1);
    installation.confirmBoot();
    installation.events.clear();

    // When - same hardware, different firmware
    BootDetails boot2 = BootDetails.of("Garo", "CPF25", "1.13", "S123", "IoT16");
    installation.registerBootNotification(boot2);

    // Then
    InstallationSnapshot snapshot = installation.toSnapshot();
    assertThat(snapshot.lastRelevantBoot()).isEqualTo(boot2);
    assertThat(snapshot.bootConfirmed()).isFalse();
    assertThat(snapshot.phase()).isEqualTo(InstallationPhase.BOOT_CONFIRMATION_REQUIRED);
    assertThat(installation.events).hasSize(1);
  }

  @Test
  void shouldNotRequireReconfirmationWhenNothingChanged() {
    // Given
    Installation installation = createInstallation();
    installation.assignDevice(DeviceIdentifier.of("DEV-12345"));
    BootDetails boot = BootDetails.of("Garo", "CPF25", "1.0", "S123", "IoT16");
    installation.registerBootNotification(boot);
    installation.confirmBoot();
    installation.events.clear();

    // When - same boot notification
    installation.registerBootNotification(boot);

    // Then - no new event (idempotent)
    assertThat(installation.events).isEmpty();
    assertThat(installation.toSnapshot().bootConfirmed()).isTrue();
  }

  @Test
  void shouldConfirmBoot() {
    // Given
    Installation installation = createInstallation();
    installation.assignDevice(DeviceIdentifier.of("DEV-12345"));
    BootDetails boot = BootDetails.of("Garo", "CPF25", "1.0", "S123", "IoT16");
    installation.registerBootNotification(boot);
    installation.events.clear();

    // When
    installation.confirmBoot();

    // Then
    InstallationSnapshot snapshot = installation.toSnapshot();
    assertThat(snapshot.bootConfirmed()).isTrue();
    assertThat(snapshot.phase()).isEqualTo(InstallationPhase.BOOT_CONFIRMED);
    assertThat(installation.events).hasSize(1);
    assertThat(installation.events.get(0)).isInstanceOf(InstallationEvent.BootConfirmed.class);
  }

  @Test
  void shouldSetLocation() {
    // Given
    Installation installation = createInstallation();
    installation.events.clear();
    Coordinates coords = Coordinates.of(16.931752852309156, 51.09836221719513);

    // When
    installation.setLocation(coords);

    // Then
    InstallationSnapshot snapshot = installation.toSnapshot();
    assertThat(snapshot.location()).isEqualTo(coords);
    assertThat(snapshot.phase()).isEqualTo(InstallationPhase.LOCATION_SET);
    assertThat(installation.events).hasSize(1);
    assertThat(installation.events.get(0)).isInstanceOf(InstallationEvent.LocationSet.class);
  }

  @Test
  void shouldFailToFinishWhenMissingRequirements() {
    // Given
    Installation installation = createInstallation();

    // When & Then
    assertThatThrownBy(() -> installation.finish())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Cannot finish")
        .hasMessageContaining("all mandatory SAT answers")
        .hasMessageContaining("precise location")
        .hasMessageContaining("boot confirmation");
  }

  @Test
  void shouldFinishInstallationWhenAllRequirementsMet() {
    // Given
    Installation installation = createInstallation();
    installation.assignInstaller(InstallerId.of("installer-1"), false);
    installation.start();
    installation.assignDevice(DeviceIdentifier.of("DEV-12345"));
    BootDetails boot = BootDetails.of("Garo", "CPF25", "1.0", "S123", "IoT16");
    installation.registerBootNotification(boot);
    installation.confirmBoot();
    installation.setLocation(Coordinates.of(16.931752852309156, 51.09836221719513));
    installation.markSatAnswersComplete();
    installation.events.clear();

    // When
    installation.finish();

    // Then
    InstallationSnapshot snapshot = installation.toSnapshot();
    assertThat(snapshot.phase()).isEqualTo(InstallationPhase.FINISHED);
    assertThat(snapshot.canFinish()).isTrue();
    assertThat(installation.events).hasSize(1);
    assertThat(installation.events.get(0))
        .isInstanceOf(InstallationEvent.InstallationFinished.class);

    InstallationEvent.InstallationFinished event =
        (InstallationEvent.InstallationFinished) installation.events.get(0);
    assertThat(event.deviceIdentifier()).isEqualTo(DeviceIdentifier.of("DEV-12345"));
    assertThat(event.ownership()).isEqualTo(Ownership.of("Devicex.nl", "public-devices"));
    assertThat(event.location()).isNotNull();
    assertThat(event.bootDetails()).isEqualTo(boot);
  }

  @Test
  void shouldBeIdempotentWhenConfirmingBootTwice() {
    // Given
    Installation installation = createInstallation();
    installation.assignDevice(DeviceIdentifier.of("DEV-12345"));
    BootDetails boot = BootDetails.of("Garo", "CPF25", "1.0", "S123", "IoT16");
    installation.registerBootNotification(boot);
    installation.confirmBoot();
    installation.events.clear();

    // When
    installation.confirmBoot();

    // Then - no new events
    assertThat(installation.events).isEmpty();
  }

  @Test
  void shouldBeIdempotentWhenAssigningSameDevice() {
    // Given
    Installation installation = createInstallation();
    DeviceIdentifier device = DeviceIdentifier.of("DEV-12345");
    installation.assignDevice(device);
    installation.events.clear();

    // When
    installation.assignDevice(device);

    // Then - no new events
    assertThat(installation.events).isEmpty();
  }

  private Installation createInstallation() {
    WorkOrderId workOrderId = WorkOrderId.of("WO-12345");
    InstallationContext context = InstallationContext.of("corporate", "POL", "wallbox", null);
    Ownership ownership = Ownership.of("Devicex.nl", "public-devices");
    return Installation.fromWorkOrder(workOrderId, context, ownership);
  }
}
