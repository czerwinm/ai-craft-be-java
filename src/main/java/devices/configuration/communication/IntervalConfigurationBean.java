package devices.configuration.communication;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AllArgsConstructor
class IntervalConfigurationBean {

  private final IntervalConfigurationProperties properties;

  @Bean
  IntervalConfiguration intervalConfiguration() {
    return properties.toIntervalConfiguration();
  }
}
