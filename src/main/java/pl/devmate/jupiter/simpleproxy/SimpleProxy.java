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

public class SimpleProxy {

    public static final String SIMPLE_PROXY_VISITED_HEADER = "simple-proxy-visited";
    private final SimpleProxyServer simpleProxyServer;

    private static final InheritableThreadLocal<SimpleProxy> defaultInstance =
            new InheritableThreadLocal<>() {
                @Override
                protected SimpleProxy initialValue() {
                    return new SimpleProxy(null);
                }
            };

    SimpleProxy(SimpleProxyServer simpleProxyServer) {
        this.simpleProxyServer = simpleProxyServer;
    }

    public static SimpleProxyContext context() {
        SimpleProxyServer simpleProxyServerInstance = defaultInstance.get().simpleProxyServer;
        if (simpleProxyServerInstance == null) {
            throw new IllegalStateException("Simple proxy server not started");
        }
        return simpleProxyServerInstance.context();
    }

    static void currentProxyServer(SimpleProxyServer simpleProxyServer) {
        defaultInstance.set(new SimpleProxy(simpleProxyServer));
    }

}
