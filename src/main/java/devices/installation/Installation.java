package devices.installation;

import devices.configuration.management.Coordinates;
import devices.configuration.management.Location;
import devices.configuration.management.Ownership;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;

@AllArgsConstructor
class Installation {

  final WorkOrderId workOrderId;
  final List<InstallationEvent> events;
  private InstallationPhase phase;
  private InstallerId assignedInstallerId;
  private final Ownership ownership;
  private final InstallationContext context;
  private DeviceIdentifier deviceIdentifier;
  private BootDetails lastRelevantBoot;
  private boolean bootConfirmed;
  private Coordinates location;
  private boolean allMandatorySatAnswered;

  static Installation fromWorkOrder(
      WorkOrderId workOrderId, InstallationContext context, Ownership ownership) {
    Installation installation =
        new Installation(
            workOrderId,
            new ArrayList<>(),
            InstallationPhase.INITIAL,
            null,
            ownership,
            context,
            null,
            null,
            false,
            null,
            false);
    installation.events.add(
        new InstallationEvent.WorkOrderReceived(workOrderId, context, ownership));
    return installation;
  }

  void assignInstaller(InstallerId installerId, boolean force) {
    Objects.requireNonNull(installerId, "InstallerId cannot be null");

    if (this.assignedInstallerId != null && !force) {
      throw new IllegalStateException("Installer already assigned. Use force=true to reassign.");
    }

    if (this.assignedInstallerId != null && force) {
      this.assignedInstallerId = installerId;
      events.add(new InstallationEvent.InstallerReassigned(workOrderId, installerId, true));
    } else {
      this.assignedInstallerId = installerId;
      events.add(new InstallationEvent.InstallerAssigned(workOrderId, installerId));
    }
  }

  void start() {
    if (assignedInstallerId == null) {
      throw new IllegalStateException("Cannot start installation without assigned installer");
    }
    if (phase != InstallationPhase.INITIAL) {
      throw new IllegalStateException("Installation already started");
    }
    events.add(new InstallationEvent.InstallationStarted(workOrderId));
  }

  void assignDevice(DeviceIdentifier newDeviceIdentifier) {
    Objects.requireNonNull(newDeviceIdentifier, "DeviceIdentifier cannot be null");

    if (this.deviceIdentifier != null && !this.deviceIdentifier.equals(newDeviceIdentifier)) {
      // Device swap - invalidate boot confirmation
      DeviceIdentifier oldDevice = this.deviceIdentifier;
      this.deviceIdentifier = newDeviceIdentifier;
      this.lastRelevantBoot = null;
      this.bootConfirmed = false;
      this.phase = InstallationPhase.DEVICE_ASSIGNED;
      events.add(new InstallationEvent.DeviceSwapped(workOrderId, oldDevice, newDeviceIdentifier));
    } else if (this.deviceIdentifier == null) {
      this.deviceIdentifier = newDeviceIdentifier;
      this.phase = InstallationPhase.DEVICE_ASSIGNED;
      events.add(new InstallationEvent.DeviceAssigned(workOrderId, newDeviceIdentifier));
    }
    // If same device - idempotent, no event
  }

  void registerBootNotification(BootDetails bootDetails) {
    Objects.requireNonNull(bootDetails, "BootDetails cannot be null");

    if (deviceIdentifier == null) {
      throw new IllegalStateException("Cannot register boot without assigned device");
    }

    boolean requiresConfirmation = isBootConfirmationRequired(bootDetails);

    if (requiresConfirmation) {
      this.lastRelevantBoot = bootDetails;
      this.bootConfirmed = false;
      this.phase = InstallationPhase.BOOT_CONFIRMATION_REQUIRED;
      events.add(new InstallationEvent.BootNotificationReceived(workOrderId, bootDetails));
    }
    // If not "relevant" (no hardware/firmware change), ignore silently
  }

  private boolean isBootConfirmationRequired(BootDetails newBoot) {
    if (lastRelevantBoot == null) {
      return true; // First boot always requires confirmation
    }
    // Re-confirm if hardware changed or firmware changed
    return lastRelevantBoot.isDifferentHardwareFrom(newBoot)
        || lastRelevantBoot.hasDifferentFirmwareFrom(newBoot);
  }

  void confirmBoot() {
    if (lastRelevantBoot == null) {
      throw new IllegalStateException("No boot notification to confirm");
    }
    if (bootConfirmed) {
      return; // Idempotent
    }

    this.bootConfirmed = true;
    this.phase = InstallationPhase.BOOT_CONFIRMED;
    events.add(new InstallationEvent.BootConfirmed(workOrderId, lastRelevantBoot));
  }

  void setLocation(Coordinates coordinates) {
    Objects.requireNonNull(coordinates, "Coordinates cannot be null");

    if (!Objects.equals(this.location, coordinates)) {
      this.location = coordinates;
      this.phase = InstallationPhase.LOCATION_SET;
      events.add(new InstallationEvent.LocationSet(workOrderId, coordinates));
    }
  }

  void markSatAnswersComplete() {
    this.allMandatorySatAnswered = true;
  }

  void finish() {
    if (!canFinish()) {
      throw new IllegalStateException(
          "Cannot finish installation. Missing: " + getMissingRequirements());
    }

    this.phase = InstallationPhase.FINISHED;

    // Build full location from coordinates (minimal for now, can be extended)
    Location fullLocation = null;
    if (location != null) {
      fullLocation = Location.of(null, null, null, null, null, null, location);
    }

    events.add(
        new InstallationEvent.InstallationFinished(
            workOrderId, deviceIdentifier, ownership, fullLocation, lastRelevantBoot));
  }

  private boolean canFinish() {
    return allMandatorySatAnswered && location != null && bootConfirmed;
  }

  private String getMissingRequirements() {
    List<String> missing = new ArrayList<>();
    if (!allMandatorySatAnswered) {
      missing.add("all mandatory SAT answers");
    }
    if (location == null) {
      missing.add("precise location");
    }
    if (!bootConfirmed) {
      missing.add("boot confirmation");
    }
    return String.join(", ", missing);
  }

  InstallationSnapshot toSnapshot() {
    return new InstallationSnapshot(
        workOrderId,
        phase,
        assignedInstallerId,
        ownership,
        context,
        deviceIdentifier,
        lastRelevantBoot,
        bootConfirmed,
        location,
        allMandatorySatAnswered,
        canFinish());
  }
}
