package devices.configuration.tools;

import devices.configuration.JsonAssert;
import io.netty.handler.logging.LogLevel;
import jakarta.annotation.PostConstruct;
import org.intellij.lang.annotations.Language;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.logging.AdvancedByteBufFormat;

@Lazy
@Component
@Profile("integration-test")
public class RequestsFixture {

    @Value("${local.server.port}")
    private int port;
    private WebClient client;
    private String jwt = null;
    public final Communication communication = new Communication();
    public final Intervals intervals = new Intervals();
    public final Installations installations = new Installations();
    public final Devices devices = new Devices();

    @PostConstruct
    void setUp() {
        this.client = WebClient.builder()
                .baseUrl("http://localhost:" + port)
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create().wiretap(this.getClass().getCanonicalName(), LogLevel.INFO, AdvancedByteBufFormat.TEXTUAL)
                ))
                .build();
    }

    public void withJwt(String jwt) {
        this.jwt = jwt;
    }

    public void clearJwt() {
        this.jwt = null;
    }

    public class Installations {
        public JsonAssert get(int page, int size) {
            return JsonAssert.assertThat(client.get()
                    .uri("/installations?page={page}&size={size}", page, size)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve().bodyToMono(String.class).block());
        }

        public JsonAssert get(String orderId1) {
            return JsonAssert.assertThat(client.get()
                    .uri("/installations/{orderId}", orderId1)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve().bodyToMono(String.class).block());
        }

        public JsonAssert patch(String orderId, @Language("JSON") String body, Object... bodyParams) {
            return JsonAssert.assertThat(client.patch()
                    .uri("/installations/{orderId}", orderId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(body.formatted(bodyParams)))
                    .retrieve().bodyToMono(String.class).block());
        }
    }

    public class Devices {
        public JsonAssert get(int page, int size) {
            return JsonAssert.assertThat(client.get()
                    .uri("/devices?page={page}&size={size}", page, size)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve().bodyToMono(String.class).block());
        }

        public JsonAssert get(String orderId) {
            return JsonAssert.assertThat(client.get()
                    .uri("/devices/{orderId}", orderId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve().bodyToMono(String.class).block());
        }

        public JsonAssert patch(String deviceId, @Language("JSON") String body, Object... bodyParams) {
            return JsonAssert.assertThat(client.patch()
                    .uri("/devices/{deviceId}", deviceId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(body.formatted(bodyParams)))
                    .retrieve().bodyToMono(String.class).block());
        }
    }

    public class Communication {

        public JsonAssert bootIot16(String deviceId, @Language("JSON") String body, Object... bodyParams) {
            return JsonAssert.assertThat(client.post()
                    .uri("/protocols/iot16/bootnotification/{deviceId}", deviceId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(body.formatted(bodyParams)))
                    .retrieve().bodyToMono(String.class).block());
        }

        public JsonAssert bootIot20(String deviceId, @Language("JSON") String body, Object... bodyParams) {
            return JsonAssert.assertThat(client.post()
                    .uri("/protocols/iot20/bootnotification/{deviceId}", deviceId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(body.formatted(bodyParams)))
                    .retrieve().bodyToMono(String.class).block());
        }
    }

    public class Intervals {

        public JsonAssert get() {
            return JsonAssert.assertThat(client.get()
                    .uri("/configs/IntervalRules")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve().bodyToMono(String.class).block());
        }

        public JsonAssert put(@Language("JSON") String body, Object... bodyParams) {
            return JsonAssert.assertThat(client.put()
                    .uri("/configs/IntervalRules")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(body.formatted(bodyParams)))
                    .retrieve().bodyToMono(String.class).block());
        }
    }
}
