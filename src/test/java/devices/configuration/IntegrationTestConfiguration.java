package devices.configuration;

import devices.configuration.communication.HeartbeatInterval;
import devices.configuration.communication.KnownDevices;
import java.time.Duration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class IntegrationTestConfiguration {

  @Bean
  @Primary
  public HeartbeatInterval testHeartbeatInterval() {
    return boot -> Duration.ofSeconds(60);
  }

  @Bean
  @Primary
  public KnownDevices testKnownDevices() {
    return deviceId -> KnownDevices.State.EXISTING;
  }
}
