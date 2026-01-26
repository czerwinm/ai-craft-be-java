package devices.configuration.tools;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.AnnotatedClassResolver;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import io.hypersistence.utils.hibernate.type.util.ObjectMapperSupplier;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

@Configuration
public class JsonConfiguration implements ObjectMapperSupplier {

  public static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper()
          .registerModule(new ParameterNamesModule(JsonCreator.Mode.PROPERTIES))
          .registerModule(new Jdk8Module())
          .registerModule(new JavaTimeModule())
          .setVisibility(PropertyAccessor.CREATOR, ANY)
          .setVisibility(PropertyAccessor.FIELD, ANY)
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
          .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
          .configure(SerializationFeature.FAIL_ON_UNWRAPPED_TYPE_IDENTIFIERS, false)
          .configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true)
          .registerModule(
              new SimpleModule().addSerializer(Page.class, new PageImplJacksonSerializer()));

  @Bean
  ObjectMapper objectMapper() {
    return OBJECT_MAPPER;
  }

  @Bean
  MappingJackson2HttpMessageConverter jacksonMessageConverter() {
    return new MappingJackson2HttpMessageConverter(OBJECT_MAPPER);
  }

  @Override
  public ObjectMapper get() {
    return OBJECT_MAPPER;
  }

  @PostConstruct
  private void initEventTypes() {
    DeserializationConfig config = OBJECT_MAPPER.getDeserializationConfig();
    AnnotationIntrospector ai = config.getAnnotationIntrospector();
    Map<Class<?>, EventTypes.Type> subtypes =
        Stream.of(devices.configuration.management.DomainEvent.class)
            .map(
                type ->
                    AnnotatedClassResolver.resolve(
                        config, OBJECT_MAPPER.constructType(type), config))
            .map(ai::findSubtypes)
            .flatMap(Collection::stream)
            .collect(
                Collectors.toMap(NamedType::getType, type -> EventTypes.Type.of(type.getName())));
    EventTypes.init(subtypes);
  }

  public static <T> T parse(String json, Class<T> type) {
    try {
      return OBJECT_MAPPER.readValue(json, type);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public static <T> T parse(JsonNode node, Class<T> type) {
    try {
      return OBJECT_MAPPER.treeToValue(node, type);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public static String json(Object object) {
    try {
      return OBJECT_MAPPER.writeValueAsString(object);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public static JsonNode jsonNode(Object object) {
    return OBJECT_MAPPER.valueToTree(object);
  }

  public record SimplePage<T>(
      List<T> content, int totalPages, int totalElements, int page, int size)
      implements Iterable<T> {
    @Override
    public Iterator<T> iterator() {
      return content.iterator();
    }
  }

  @JsonComponent
  static class PageImplJacksonSerializer extends JsonSerializer<Page> {
    @Override
    public void serialize(
        final Page page, final JsonGenerator jsonGenerator, final SerializerProvider serializers)
        throws IOException {
      jsonGenerator.writeStartObject();
      jsonGenerator.writeObjectField("content", page.getContent());
      jsonGenerator.writeNumberField("totalPages", page.getTotalPages());
      jsonGenerator.writeNumberField("totalElements", page.getTotalElements());
      jsonGenerator.writeNumberField("page", page.getNumber());
      jsonGenerator.writeNumberField("size", page.getNumberOfElements());
      jsonGenerator.writeEndObject();
    }
  }
}
