package devices.configuration.tools;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import io.netty.handler.logging.LogLevel;
import jakarta.annotation.PreDestroy;
import java.util.Objects;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.logging.AdvancedByteBufFormat;

@Component
@Profile("auth-test")
public class AuthFixture {

  private final KeycloakContainer keycloak;
  private final WebClient client;

  public AuthFixture() {
    keycloak = new KeycloakContainer().withReuse(true).withRealmImportFile("/iot-realm.json");
    keycloak.start();
    System.setProperty(
        "spring.security.oauth2.resourceserver.jwt.issuer-uri",
        Objects.requireNonNull(keycloak.getAuthServerUrl()) + "/realms/iot");

    client =
        WebClient.builder()
            .baseUrl(keycloak.getAuthServerUrl())
            .clientConnector(
                new ReactorClientHttpConnector(
                    HttpClient.create()
                        .wiretap(
                            this.getClass().getCanonicalName(),
                            LogLevel.INFO,
                            AdvancedByteBufFormat.TEXTUAL)))
            .build();
  }

  @PreDestroy
  public void clean() {
    if (!keycloak.isShouldBeReused()) {
      keycloak.stop();
    }
  }

  public String tokenFor(String username, String password) {
    AccessToken response =
        client
            .post()
            .uri("realms/iot/protocol/openid-connect/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(
                BodyInserters.fromFormData("scope", "openid")
                    .with("username", username)
                    .with("password", password)
                    .with("grant_type", "password")
                    .with("client_id", "iot-service")
                    .with("client_secret", "secret"))
            .retrieve()
            .bodyToMono(AccessToken.class)
            .block();
    Objects.requireNonNull(response, "auth response from keycloak");
    return response.access_token;
  }

  record AccessToken(
      String access_token,
      String refresh_token,
      String id_token,
      String token_type,
      int expires_in,
      String scope) {}
}
