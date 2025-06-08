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

/**
 * Configuration parameters for SimpleProxy
 * @param port port for http proxy, default {@code 0} (random port)
 * @param addProxyResponseHeader should add header to response {@link SimpleProxy#SIMPLE_PROXY_VISITED_HEADER}, default {@code true}
 * @param storeRequestBody should request body be captured in proxy events, default {@code false}
 * @param storeResponseBody should response body be captured in proxy events, default {@code false}
 */
public record SimpleProxyConfig(
        int port,
        boolean addProxyResponseHeader,
        boolean storeRequestBody,
        boolean storeResponseBody) {

    public static final SimpleProxyConfig DEFAULT = new SimpleProxyConfig(
            0,
            true,
            false,
            false
    );

    public static SimpleProxyConfigBuilder builder() {
        return new SimpleProxyConfigBuilder();
    }

}
