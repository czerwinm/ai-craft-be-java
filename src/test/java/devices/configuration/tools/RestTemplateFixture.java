package devices.configuration.tools;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.StreamingHttpOutputMessage;
import org.springframework.http.client.*;
import org.springframework.stereotype.Component;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;
import java.util.function.Function;

@Lazy
@Component
@Profile("integration-test")
public class RestTemplateFixture {

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private ApplicationContext context;

    public RestTemplateManipulation getRestTemplate(String beanName, String restTemplateFieldName) {
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(context.getBean(beanName), restTemplateFieldName);
        return new RestTemplateManipulation(restTemplate);
    }

    public RestTemplateManipulation getRestTemplate(Class<?> beanType, String restTemplateFieldName) {
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(context.getBean(beanType), restTemplateFieldName);
        return new RestTemplateManipulation(restTemplate);
    }

    public class RestTemplateManipulation {
        private final RestTemplate restTemplate;

        public RestTemplateManipulation(RestTemplate restTemplate) {
            this.restTemplate = restTemplate;
        }

        public MockRestServiceServer mock() {
            return MockRestServiceServer.createServer(restTemplate);
        }

        public RestTemplateManipulation overrideToLocalServer() {
            return override("localhost", port);
        }

        public RestTemplateManipulation override(String host, int port) {
            return override(uri -> uri.host(host).port(port));
        }

        public RestTemplateManipulation override(@NotNull Function<@NotNull UriComponentsBuilder, @NotNull UriComponentsBuilder> function) {
            ClientHttpRequestFactory requestFactory = restTemplate.getRequestFactory();
            ClientHttpRequestFactory overriding = new ClientHttpRequestFactory() {
                @Override
                public @NotNull ClientHttpRequest createRequest(@NotNull URI uri, @NotNull HttpMethod httpMethod) throws IOException {
                    return requestFactory.createRequest(
                            function.apply(UriComponentsBuilder.fromUri(uri)).build().toUri(),
                            httpMethod
                    );
                }
            };
            restTemplate.setRequestFactory(overriding);
            return this;
        }

        public RestTemplateManipulation intercept(ClientHttpRequestInterceptor interceptor) {
            restTemplate.setRequestFactory(new InterceptingClientHttpRequestFactory(
                    restTemplate.getRequestFactory(),
                    List.of(interceptor)
            ));
            return this;
        }

        public MockRestServiceServer interceptToMockRestServiceServer() {
            ClientHttpRequestFactory original = restTemplate.getRequestFactory();
            MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
            ClientHttpRequestFactory mocked = restTemplate.getRequestFactory();
            restTemplate.setRequestFactory(original);
            intercept(new MockRestServerInterceptor(mocked));
            return server;
        }

        private record MockRestServerInterceptor(
                ClientHttpRequestFactory mocked) implements ClientHttpRequestInterceptor {

            @Override
            public @NotNull ClientHttpResponse intercept(HttpRequest request, byte[] body, @NotNull ClientHttpRequestExecution execution) throws IOException {
                ClientHttpRequest delegate = mocked.createRequest(request.getURI(), request.getMethod());
                request.getHeaders().forEach((key, value) -> delegate.getHeaders().addAll(key, value));
                request.getAttributes().forEach((key, value) -> delegate.getAttributes().put(key, value));
                if (body.length > 0) {
                    long contentLength = delegate.getHeaders().getContentLength();
                    if (contentLength > -1 && contentLength != body.length) {
                        delegate.getHeaders().setContentLength(body.length);
                    }
                    if (delegate instanceof StreamingHttpOutputMessage streamingOutputMessage) {
                        streamingOutputMessage.setBody(new StreamingHttpOutputMessage.Body() {
                            @Override
                            public void writeTo(@NotNull OutputStream outputStream) throws IOException {
                                StreamUtils.copy(body, outputStream);
                            }

                            @Override
                            public boolean repeatable() {
                                return true;
                            }
                        });
                    } else {
                        StreamUtils.copy(body, delegate.getBody());
                    }
                }
                delegate.execute().close();
                return execution.execute(request, body);
            }
        }
    }
}
