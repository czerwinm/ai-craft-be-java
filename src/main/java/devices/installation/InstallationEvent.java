package devices.installation;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import devices.configuration.management.Coordinates;
import devices.configuration.management.Location;
import devices.configuration.management.Ownership;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(
      value = InstallationEvent.WorkOrderReceived.class,
      name = "WorkOrderReceived_v1"),
  @JsonSubTypes.Type(
      value = InstallationEvent.InstallerAssigned.class,
      name = "InstallerAssigned_v1"),
  @JsonSubTypes.Type(
      value = InstallationEvent.InstallerReassigned.class,
      name = "InstallerReassigned_v1"),
  @JsonSubTypes.Type(
      value = InstallationEvent.InstallationStarted.class,
      name = "InstallationStarted_v1"),
  @JsonSubTypes.Type(value = InstallationEvent.DeviceAssigned.class, name = "DeviceAssigned_v1"),
  @JsonSubTypes.Type(value = InstallationEvent.DeviceSwapped.class, name = "DeviceSwapped_v1"),
  @JsonSubTypes.Type(
      value = InstallationEvent.BootNotificationReceived.class,
      name = "BootNotificationReceived_v1"),
  @JsonSubTypes.Type(value = InstallationEvent.BootConfirmed.class, name = "BootConfirmed_v1"),
  @JsonSubTypes.Type(value = InstallationEvent.LocationSet.class, name = "LocationSet_v1"),
  @JsonSubTypes.Type(
      value = InstallationEvent.InstallationFinished.class,
      name = "InstallationFinished_v1")
})
public sealed interface InstallationEvent {

  WorkOrderId workOrderId();

  record WorkOrderReceived(
      WorkOrderId workOrderId, InstallationContext context, Ownership ownership)
      implements InstallationEvent {}

  record InstallerAssigned(WorkOrderId workOrderId, InstallerId installerId)
      implements InstallationEvent {}

  record InstallerReassigned(WorkOrderId workOrderId, InstallerId installerId, boolean forced)
      implements InstallationEvent {}

  record InstallationStarted(WorkOrderId workOrderId) implements InstallationEvent {}

  record DeviceAssigned(WorkOrderId workOrderId, DeviceIdentifier deviceIdentifier)
      implements InstallationEvent {}

  record DeviceSwapped(
      WorkOrderId workOrderId,
      DeviceIdentifier oldDeviceIdentifier,
      DeviceIdentifier newDeviceIdentifier)
      implements InstallationEvent {}

  record BootNotificationReceived(WorkOrderId workOrderId, BootDetails bootDetails)
      implements InstallationEvent {}

  record BootConfirmed(WorkOrderId workOrderId, BootDetails bootDetails)
      implements InstallationEvent {}

  record LocationSet(WorkOrderId workOrderId, Coordinates coordinates)
      implements InstallationEvent {}

  record InstallationFinished(
      WorkOrderId workOrderId,
      DeviceIdentifier deviceIdentifier,
      Ownership ownership,
      Location location,
      BootDetails bootDetails)
      implements InstallationEvent {}
}
