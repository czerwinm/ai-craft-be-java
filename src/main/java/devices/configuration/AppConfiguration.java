package devices.configuration;

import java.time.Clock;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConfigurationPropertiesScan
@ComponentScan(basePackages = {"devices.configuration", "devices.installation"})
@EnableJpaRepositories(basePackages = "devices", considerNestedRepositories = true)
@EntityScan(basePackages = "devices")
class AppConfiguration {

  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }
}
