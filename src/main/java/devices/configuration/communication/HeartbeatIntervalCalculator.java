package devices.configuration.communication;

import java.time.Duration;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class HeartbeatIntervalCalculator implements HeartbeatInterval {

  private final IntervalConfiguration configuration;

  @Override
  public Duration heartbeatIntervalFor(BootNotification boot) {
    return configuration.calculateInterval(boot);
  }
}
