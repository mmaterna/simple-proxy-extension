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

public class SimpleProxyConfigBuilder {

    private int port = 0;
    private boolean addProxyResponseHeader = true;
    private boolean storeRequestBody = false;
    private boolean storeResponseBody = false;

    /**
     * Local port for http proxy, {@code 0} means random port.
     * @param port local port
     */
    public SimpleProxyConfigBuilder port(int port) {
        this.port = port;
        return this;
    }

    /**
     * Whether to add the custom response header {@link SimpleProxy#SIMPLE_PROXY_VISITED_HEADER}
     * @param addProxyResponseHeader true if the custom header to be added to every response.
     */
    public SimpleProxyConfigBuilder addProxyResponseHeader(boolean addProxyResponseHeader) {
        this.addProxyResponseHeader = addProxyResponseHeader;
        return this;
    }

    /**
     * Whether to try to capture the content of the request.
     * @param storeRequestBody true if request body content to be captured
     */
    public SimpleProxyConfigBuilder storeRequestBody(boolean storeRequestBody) {
        this.storeRequestBody = storeRequestBody;
        return this;
    }

    /**
     * Whether to try to capture the content of the response.
     * @param storeResponseBody true if response body content to be captured
     */
    public SimpleProxyConfigBuilder storeResponseBody(boolean storeResponseBody) {
        this.storeResponseBody = storeResponseBody;
        return this;
    }

    public SimpleProxyConfig build() {
        return new SimpleProxyConfig(
                port,
                addProxyResponseHeader,
                storeRequestBody,
                storeResponseBody
        );
    }

}
