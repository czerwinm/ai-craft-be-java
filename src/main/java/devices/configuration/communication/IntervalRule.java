package devices.configuration.communication;

import java.time.Duration;
import java.util.Set;
import java.util.regex.Pattern;

public sealed interface IntervalRule {

  Duration interval();

  boolean matches(BootNotification boot);

  record DeviceIdRule(Duration interval, Set<String> deviceIds) implements IntervalRule {

    public DeviceIdRule {
      if (interval == null || interval.isNegative()) {
        throw new IllegalArgumentException("Interval must be positive");
      }
      if (deviceIds == null || deviceIds.isEmpty()) {
        throw new IllegalArgumentException("Device IDs cannot be empty");
      }
    }

    @Override
    public boolean matches(BootNotification boot) {
      return deviceIds.contains(boot.deviceId());
    }
  }

  record ModelRule(Duration interval, String vendor, String modelRegex) implements IntervalRule {

    public ModelRule {
      if (interval == null || interval.isNegative()) {
        throw new IllegalArgumentException("Interval must be positive");
      }
      if (vendor == null || vendor.isBlank()) {
        throw new IllegalArgumentException("Vendor cannot be empty");
      }
      if (modelRegex == null || modelRegex.isBlank()) {
        throw new IllegalArgumentException("Model regex cannot be empty");
      }
    }

    @Override
    public boolean matches(BootNotification boot) {
      if (!vendor.equals(boot.vendor())) {
        return false;
      }
      try {
        Pattern pattern = Pattern.compile(modelRegex);
        return pattern.matcher(boot.model()).matches();
      } catch (Exception e) {
        return false;
      }
    }
  }

  record DefaultRule(Duration interval) implements IntervalRule {

    public DefaultRule {
      if (interval == null || interval.isNegative()) {
        throw new IllegalArgumentException("Interval must be positive");
      }
    }

    @Override
    public boolean matches(BootNotification boot) {
      return true;
    }
  }
}
