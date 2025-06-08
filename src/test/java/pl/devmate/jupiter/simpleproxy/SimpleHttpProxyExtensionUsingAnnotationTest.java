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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.devmate.jupiter.simpleproxy.events.ProxyEvent;
import pl.devmate.jupiter.simpleproxy.events.ProxyEventsCaptured;

import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.devmate.jupiter.simpleproxy.TestHelper.*;

@WireMockTest
@SimpleProxyTest(storeRequestBody = true, storeResponseBody = true)
class SimpleHttpProxyExtensionUsingAnnotationTest {

    private static final Logger log = LoggerFactory.getLogger(SimpleHttpProxyExtensionUsingAnnotationTest.class);

    @Test
    void injectedContextAndStaticValuesShouldBeEqual(SimpleProxyExtension simpleProxyExtension) {
        assertThat(SimpleProxy.runtimeInfo()).isEqualTo(simpleProxyExtension.runtimeInfo());
    }

    @Test
    void extensionShouldRegisterProxyOnNonZeroPort() {
        assertThat(SimpleProxy.runtimeInfo().httpPort()).isGreaterThan(0);
    }

    @Test
    void twoRequests(WireMockRuntimeInfo wmRuntimeInfo, SimpleProxyExtension simpleProxyExtension) throws Exception {
        mockHttpServerGetResponse();
        mockHttpServerPostResponse();
        String endpointUrl = wmRuntimeInfo.getHttpBaseUrl() + MOCKED_PATH;

        log.info("=============== GET request ===============");
        // first request using proxy
        HttpResponse<String> response1 = sendGetRequestWithProxy(
                simpleProxyExtension.buildHttpProxySelector(),
                endpointUrl);

        assertThat(response1.statusCode()).isEqualTo(200);

        log.info("=============== POST request ===============");
        // second request using proxy
        HttpResponse<String> response2 = sendPostRequestWithProxy(
                simpleProxyExtension.buildHttpProxySelector(),
                endpointUrl,
                "request body"
        );
        assertThat(response2.statusCode()).isEqualTo(200);

        // --- check captured events
        ProxyEventsCaptured events = simpleProxyExtension.eventsCaptured();

        assertThat(events.proxyEvents()).hasSize(2);

        ProxyEvent event1 = events.proxyEvents().get(0);
        assertThat(event1.clientToProxyRequest().method()).isEqualTo("GET");
        assertThat(event1.clientToProxyRequest().requestBytes()).isNull();
        assertThat(event1.serverToProxyResponse().responseBytes()).isNotEmpty();


        ProxyEvent event2 = events.proxyEvents().get(1);
        assertThat(event2.clientToProxyRequest().method()).isEqualTo("POST");
        assertThat(event2.clientToProxyRequest().requestBytes()).isNotEmpty();
        assertThat(event2.serverToProxyResponse().responseBytes()).isNotEmpty();
    }

}
