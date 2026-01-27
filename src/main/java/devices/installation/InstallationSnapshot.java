package devices.installation;

import devices.configuration.management.Coordinates;
import devices.configuration.management.Ownership;

public record InstallationSnapshot(
    WorkOrderId workOrderId,
    InstallationPhase phase,
    InstallerId assignedInstallerId,
    Ownership ownership,
    InstallationContext context,
    DeviceIdentifier deviceIdentifier,
    BootDetails lastRelevantBoot,
    boolean bootConfirmed,
    Coordinates location,
    boolean allMandatorySatAnswered,
    boolean canFinish) {}
