package devices.configuration.tools;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.awaitility.Awaitility;
import org.intellij.lang.annotations.Language;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@Component
@Profile("kafka-test")
@AllArgsConstructor
public class KafkaFixture {

  private final KafkaTemplate<String, String> kafka;
  private final List<ConsumerRecord<String, String>> received = new ArrayList<>();

  @SneakyThrows
  public void publish(String topic, String key, @Language("JSON") String message, Object... args) {
    SendResult<String, String> result =
        kafka.send(new ProducerRecord<>(topic, key, message.formatted(args))).get();
    long offset = result.getRecordMetadata().offset();
    Awaitility.await("kafka message")
        .until(
            () ->
                received.reversed().parallelStream()
                    .anyMatch(record -> record.topic().equals(topic) && record.offset() == offset));
  }

  @KafkaListener(topicPattern = ".*", groupId = "KafkaFixture")
  void handle(ConsumerRecord<String, String> message) {
    received.add(message);
  }

  @Configuration
  @Profile("!kafka-test")
  @EnableAutoConfiguration(exclude = KafkaAutoConfiguration.class)
  static class KafkaMockConfiguration {
    @Bean
    @Primary
    KafkaTemplate kafka() {
      return Mockito.mock(KafkaTemplate.class);
    }
  }

  @Configuration
  @Profile("kafka-test")
  static class KafkaConfiguration {
    public KafkaConfiguration() {
      KafkaContainer kafkaContainer =
          new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.3.0"))
              .withReuse(true)
              .withNetwork(null);
      kafkaContainer.start();

      System.setProperty("spring.kafka.bootstrap-servers", kafkaContainer.getBootstrapServers());
    }
  }
}
