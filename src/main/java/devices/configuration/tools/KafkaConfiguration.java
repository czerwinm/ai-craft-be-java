package devices.configuration.tools;

import static org.apache.kafka.clients.CommonClientConfigs.*;
import static org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.CommonClientConfigs.GROUP_ID_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.*;
import static org.apache.kafka.common.config.SaslConfigs.SASL_MECHANISM;
import static org.apache.kafka.common.config.SslConfigs.*;
import static org.apache.kafka.common.security.auth.SecurityProtocol.SASL_SSL;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.CommonLoggingErrorHandler;

@AllArgsConstructor
@Configuration
@EnableConfigurationProperties(KafkaProperties.class)
class KafkaConfiguration {

  private KafkaProperties kafkaProperties;

  @Bean
  ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
    return concurrentListenerContainerFactory(
        new CommonLoggingErrorHandler(), new StringDeserializer());
  }

  private <T> ConcurrentKafkaListenerContainerFactory<String, T> concurrentListenerContainerFactory(
      CommonErrorHandler errorHandler, Deserializer<T> valueDeserializer) {
    HashMap<String, Object> props = new HashMap<>();
    KafkaProperties.Consumer consumer = kafkaProperties.getConsumer();

    setupBootstrapServers(props);
    props.put(GROUP_ID_CONFIG, consumer.getGroupId());
    props.put(AUTO_OFFSET_RESET_CONFIG, consumer.getAutoOffsetReset());
    props.put(MAX_POLL_RECORDS_CONFIG, 400);
    props.put(FETCH_MAX_BYTES_CONFIG, 1 * 1024 * 1024);

    setupSsl(props, consumer.getProperties(), consumer.getSsl());

    var consumerFactory =
        new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), valueDeserializer);

    var factory = new ConcurrentKafkaListenerContainerFactory<String, T>();
    factory.setConsumerFactory(consumerFactory);
    factory.setCommonErrorHandler(errorHandler);
    return factory;
  }

  @Bean
  ProducerFactory defaultProducerFactory() {
    return producerFactory(new StringSerializer());
  }

  private <T> DefaultKafkaProducerFactory<String, T> producerFactory(Serializer valueSerializer) {
    HashMap<String, Object> props = new HashMap<>();
    KafkaProperties.Producer producer = kafkaProperties.getProducer();
    setupBootstrapServers(props);

    setupSsl(props, producer.getProperties(), producer.getSsl());

    return new DefaultKafkaProducerFactory<String, T>(
        props, new StringSerializer(), valueSerializer);
  }

  private void setupBootstrapServers(HashMap<String, Object> props) {
    props.put(BOOTSTRAP_SERVERS_CONFIG, String.join(",", kafkaProperties.getBootstrapServers()));
  }

  private void setupSsl(
      HashMap<String, Object> props, Map<String, String> properties, KafkaProperties.Ssl ssl) {
    if (isSslSecurityEnabled(kafkaProperties)) {
      props.put(SASL_MECHANISM, properties.get(SASL_MECHANISM));
      props.put(SECURITY_PROTOCOL_CONFIG, kafkaProperties.getSecurity().getProtocol());
      props.put(SSL_TRUSTSTORE_LOCATION_CONFIG, resourceToPath(ssl.getTrustStoreLocation()));
      props.put(SSL_TRUSTSTORE_PASSWORD_CONFIG, ssl.getTrustStorePassword());
      props.put(
          SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG,
          kafkaProperties.getProperties().get(SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG));
    }
  }

  private boolean isSslSecurityEnabled(KafkaProperties kafkaProperties) {
    return SASL_SSL.name.equals(kafkaProperties.getSecurity().getProtocol());
  }

  private String resourceToPath(Resource resource) {
    try {
      return resource.getFile().getAbsolutePath();
    } catch (IOException ex) {
      throw new IllegalStateException("Resource '$resource' must be on a file system", ex);
    }
  }
}
