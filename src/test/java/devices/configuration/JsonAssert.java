package devices.configuration;

import devices.configuration.tools.JsonConfiguration;
import lombok.SneakyThrows;
import org.intellij.lang.annotations.Language;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONAssert;

public class JsonAssert {

  private final String actual;

  public JsonAssert(String actual) {
    this.actual = actual;
  }

  public static JsonAssert assertThat(Object object) {
    return new JsonAssert(json(object));
  }

  public static JsonAssert assertThat(@Language("JSON") String json) {
    return new JsonAssert(json);
  }

  @SneakyThrows
  public JsonAssert isExactlyLike(Object object) {
    assertJson(json(object), actual, true);
    return this;
  }

  @SneakyThrows
  public JsonAssert isExactlyLike(@Language("JSON") String json) {
    assertJson(json, actual, true);
    return this;
  }

  @SneakyThrows
  public JsonAssert isExactlyLike(@Language("JSON") String json, Object... params) {
    assertJson(json.formatted(params), actual, true);
    return this;
  }

  @SneakyThrows
  public JsonAssert hasFieldsLike(Object object) {
    assertJson(json(object), actual, false);
    return this;
  }

  @SneakyThrows
  public JsonAssert hasFieldsLike(@Language("JSON") String json) {
    assertJson(json, actual, false);
    return this;
  }

  @SneakyThrows
  public JsonAssert hasFieldsLike(@Language("JSON") String json, Object... params) {
    assertJson(json.formatted(params), actual, false);
    return this;
  }

  @SneakyThrows
  public static String json(Object object) {
    return JsonConfiguration.OBJECT_MAPPER.writeValueAsString(object);
  }

  private static void assertJson(String expected, String actual, boolean strict)
      throws JSONException {
    try {
      JSONAssert.assertEquals(expected, actual, strict);
    } catch (Throwable e) {
      System.err.println("Actual:");
      System.err.println(actual);
      throw e;
    }
  }
}
