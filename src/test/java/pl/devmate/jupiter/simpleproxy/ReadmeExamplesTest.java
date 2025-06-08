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

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import pl.devmate.jupiter.simpleproxy.events.ProxyEvent;
import pl.devmate.jupiter.simpleproxy.events.ProxyEventsCaptured;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled("Not real tests, readme examples")
@WireMockTest
@SimpleProxyTest(storeRequestBody = true, storeResponseBody = true) // starts forward proxy on random port before all tests
class ReadmeExamplesTest {

    /**
     * Creates proxy extension based on custom configuration
     */
    @RegisterExtension
    static SimpleProxyExtension simpleProxyExtension = new SimpleProxyExtension(SimpleProxyConfig.builder()
            .port(0)
            .addProxyResponseHeader(true)
            .storeRequestBody(true)
            .storeResponseBody(true)
            .build()
    );

    @Test
    void checkProxy() throws Exception {
        // get proxy configuration
        HttpClient client = HttpClient.newBuilder().proxy(SimpleProxy.buildHttpProxySelector()).build();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create("http://example.com")).GET().build();

        // send request through proxy
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // http response should contain header SimpleProxy.SIMPLE_PROXY_VISITED_HEADER
        assertThat(response.headers().firstValue(SimpleProxy.SIMPLE_PROXY_VISITED_HEADER)).hasValue("true");
    }

    @Test
    void checkExample(SimpleProxyExtension simpleProxyExtension) throws Exception {
        HttpClient client = HttpClient.newBuilder().proxy(simpleProxyExtension.buildHttpProxySelector()).build();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create("http://example.com")).GET().build();

        // send request through proxy
        client.send(request, HttpResponse.BodyHandlers.ofString());

        // --- check captured events
        ProxyEventsCaptured events = simpleProxyExtension.eventsCaptured();

        assertThat(events.proxyEvents()).hasSize(1);
        ProxyEvent proxyEvent = events.proxyEvents().get(0);
        assertThat(proxyEvent.clientToProxyRequest().method()).isEqualTo("GET");
        assertThat(proxyEvent.clientToProxyRequest().requestBytes()).isNull();
        assertThat(proxyEvent.serverToProxyResponse().responseBytes()).isNotEmpty();
        assertThat(proxyEvent.serverToProxyResponse().httpStatusCode()).isEqualTo(200);
    }

}
