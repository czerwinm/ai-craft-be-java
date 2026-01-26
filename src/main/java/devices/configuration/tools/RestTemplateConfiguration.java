package devices.configuration.tools;

import static java.time.Duration.ofSeconds;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
class RestTemplateConfiguration {

  @Bean
  RestTemplate restTemplate(
      @Value("${client.read-timeout:5}") int readTimeout,
      @Value("${client.connect-timeout:5}") int connectTimeout,
      RestTemplateBuilder restTemplateBuilder) {
    return restTemplateBuilder
        .readTimeout(ofSeconds(readTimeout))
        .connectTimeout(ofSeconds(connectTimeout))
        .build();
  }
}
