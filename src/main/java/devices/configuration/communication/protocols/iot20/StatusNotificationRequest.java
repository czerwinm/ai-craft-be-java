package devices.configuration.communication.protocols.iot20;

import devices.configuration.communication.DeviceStatuses;
import java.util.List;

record StatusNotificationRequest(List<String> statuses) {
  DeviceStatuses toStatusNotificationEvent(String deviceId) {
    return new DeviceStatuses(deviceId, statuses);
  }
}
