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

import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.devmate.jupiter.simpleproxy.TestHelper.*;
import static pl.devmate.jupiter.simpleproxy.SimpleProxy.SIMPLE_PROXY_VISITED_HEADER;

@WireMockTest
class SimpleHttpProxyExtensionTest {

    @RegisterExtension
    static SimpleProxyExtension simpleProxyExtension = SimpleProxyExtension.builder().build();

    @Test
    void injectedContextAndStaticValuesShouldBeEqual(SimpleProxyContext simpleProxyContext) {
        assertThat(SimpleProxy.context())
                .isEqualTo(simpleProxyContext)
                .isEqualTo(simpleProxyExtension.context());
    }

    @Test
    void checkProxy(WireMockRuntimeInfo wmRuntimeInfo, SimpleProxyContext simpleProxyContext) throws Exception {
        mockHttpServerResponse();
        String endpointUrl = wmRuntimeInfo.getHttpBaseUrl() + MOCKED_PATH;

        HttpResponse<String> response = sendRequestWithProxy(
                simpleProxyContext.getProxySelector(),
                endpointUrl);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue(SIMPLE_PROXY_VISITED_HEADER)).hasValue("true");
        assertThat(response.body()).isEqualTo(MOCKED_RESPONSE_BODY);
    }

}
