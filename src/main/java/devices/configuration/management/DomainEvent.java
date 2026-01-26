package devices.configuration.management;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = DomainEvent.DeviceCreated.class, name = "DeviceCreated_v1"),
  @JsonSubTypes.Type(value = DomainEvent.OwnershipChanged.class, name = "OwnershipChanged_v1"),
  @JsonSubTypes.Type(value = DomainEvent.LocationChanged.class, name = "LocationChanged_v1"),
  @JsonSubTypes.Type(
      value = DomainEvent.OpeningHoursChanged.class,
      name = "OpeningHoursChanged_v1"),
  @JsonSubTypes.Type(value = DomainEvent.SettingsChanged.class, name = "SettingsChanged_v1"),
  @JsonSubTypes.Type(
      value = DomainEvent.DeviceConfigurationChanged.class,
      name = "DeviceConfigurationChanged_v1")
})
public sealed interface DomainEvent {

  String deviceId();

  record DeviceCreated(String deviceId) implements DomainEvent {}

  record OwnershipChanged(String deviceId, Ownership ownership) implements DomainEvent {}

  record LocationChanged(String deviceId, Location location) implements DomainEvent {}

  record OpeningHoursChanged(String deviceId, OpeningHours openingHours) implements DomainEvent {}

  record SettingsChanged(String deviceId, Settings settings) implements DomainEvent {}

  record DeviceConfigurationChanged(String deviceId) implements DomainEvent {}
}
