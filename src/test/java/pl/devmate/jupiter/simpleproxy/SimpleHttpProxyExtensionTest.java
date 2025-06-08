/*
 * Copyright © 2025 Mariusz Materna
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package pl.devmate.jupiter.simpleproxy;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.devmate.jupiter.simpleproxy.events.ProxyEvent;
import pl.devmate.jupiter.simpleproxy.events.ProxyEventHttpHeaders;
import pl.devmate.jupiter.simpleproxy.events.ProxyEventsCaptured;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.devmate.jupiter.simpleproxy.SimpleProxy.SIMPLE_PROXY_VISITED_HEADER;
import static pl.devmate.jupiter.simpleproxy.TestHelper.*;

@WireMockTest(httpsEnabled = true)
class SimpleHttpProxyExtensionTest {

    private static final Logger log = LoggerFactory.getLogger(SimpleHttpProxyExtensionTest.class);

    @RegisterExtension
    static SimpleProxyExtension simpleProxyExtension = new SimpleProxyExtension(SimpleProxyConfig.builder()
            .port(0)
            .addProxyResponseHeader(true)
            .storeRequestBody(true)
            .storeResponseBody(true)
            .build()
    );

    @Test
    void injectedContextAndStaticValuesShouldBeEqual(SimpleProxyExtension simpleProxyExtension) {
        assertThat(SimpleProxy.runtimeInfo())
                .isEqualTo(simpleProxyExtension.runtimeInfo())
                .isEqualTo(SimpleHttpProxyExtensionTest.simpleProxyExtension.runtimeInfo());
    }

    @Test
    void checkProxyHttpGet(WireMockRuntimeInfo wmRuntimeInfo, SimpleProxyExtension simpleProxyExtension) throws Exception {
        mockHttpServerGetResponse();
        String endpointUrl = wmRuntimeInfo.getHttpBaseUrl() + MOCKED_PATH;

        HttpResponse<String> response = sendGetRequestWithProxy(
                simpleProxyExtension.buildHttpProxySelector(),
                endpointUrl);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue(SIMPLE_PROXY_VISITED_HEADER)).hasValue("true");
        assertThat(response.body()).isEqualTo(MOCKED_GET_RESPONSE_BODY);
    }

    @Test
    void checkProxyHttpPost(WireMockRuntimeInfo wmRuntimeInfo, SimpleProxyExtension simpleProxyExtension) throws Exception {
        mockHttpServerPostResponse();
        String endpointUrl = wmRuntimeInfo.getHttpBaseUrl() + MOCKED_PATH;

        HttpResponse<String> response = sendPostRequestWithProxy(
                simpleProxyExtension.buildHttpProxySelector(),
                endpointUrl,
                "request body"
        );

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue(SIMPLE_PROXY_VISITED_HEADER)).hasValue("true");
        assertThat(response.body()).isEqualTo(MOCKED_POST_RESPONSE_BODY);
        assertThat(SimpleProxy.eventsCaptured().proxyEvents()).hasSize(1);

        ProxyEvent event = SimpleProxy.eventsCaptured().proxyEvents().get(0);

        assertThat(event.clientToProxyRequest().uri().toString()).contains(MOCKED_PATH);
        assertThat(event.clientToProxyRequest().instant()).isNotNull();
        assertThat(event.clientToProxyRequest().method()).isEqualTo("POST");

        ProxyEventHttpHeaders requestHeaders = event.clientToProxyRequest().headers();
        assertThat(requestHeaders.findByName(SIMPLE_PROXY_VISITED_HEADER)).isEmpty();
        assertThat(requestHeaders.headers()).hasSize(3);
        assertThat(requestHeaders.findByName("Content-Length")).isPresent();
        assertThat(requestHeaders.findByName("Host")).isPresent();
        assertThat(requestHeaders.findByName("User-Agent")).isPresent();

        assertThat(event.serverToProxyResponse().httpStatusCode()).isEqualTo(200);
        assertThat(event.serverToProxyResponse().instant()).isNotNull();
        ProxyEventHttpHeaders serverResponseHeaders = event.serverToProxyResponse().headers();
        assertThat(serverResponseHeaders.headers()).hasSize(4);
        assertThat(serverResponseHeaders.findByName("Matched-Stub-Id")).isPresent();
        assertThat(serverResponseHeaders.findByName("Vary")).isPresent();
        assertThat(serverResponseHeaders.findByName("Transfer-Encoding")).isPresent();
        assertThat(serverResponseHeaders.findByName("Content-Type")).isPresent();
        assertThat(event.serverToProxyResponse().responseBytes()).isNotEmpty();
        assertThat(event.clientToProxyRequest().requestBytes()).isNotEmpty();
    }

    @Test
    void shouldCaptureConnectEventForHttpsEndpoint(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
        mockHttpServerGetResponse();

        HttpClient client = HttpClient.newBuilder()
                .proxy(simpleProxyExtension.buildHttpProxySelector())
                .sslContext(prepareAcceptAllSslContext())
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(wmRuntimeInfo.getHttpsBaseUrl() + MOCKED_PATH))
                .GET().build();

        // send request through proxy to https endpoint (only connect captured)
        client.send(request, HttpResponse.BodyHandlers.ofString());

        // --- check captured events
        ProxyEventsCaptured events = simpleProxyExtension.eventsCaptured();

        events.proxyEvents().forEach(event -> log.debug("proxy event: {}", event));
        assertThat(events.proxyEvents()).hasSize(1);
        ProxyEvent proxyEvent = events.proxyEvents().get(0);
        assertThat(proxyEvent.clientToProxyRequest().method()).isEqualTo("CONNECT");
        assertThat(proxyEvent.clientToProxyRequest().requestBytes()).isNull();
        assertThat(proxyEvent.serverToProxyResponse().responseBytes()).isNull();
        assertThat(proxyEvent.serverToProxyResponse().httpStatusCode()).isEqualTo(200);
    }

}
