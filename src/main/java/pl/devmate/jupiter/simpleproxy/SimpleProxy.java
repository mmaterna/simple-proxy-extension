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

import pl.devmate.jupiter.simpleproxy.events.ProxyEventsCaptured;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;

/**
 * Utility class with static access to registered JUnit Jupiter extension {@link SimpleProxyExtension}.
 */
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

    public static SimpleProxyRuntimeInfo runtimeInfo() {
        return runningSimpleProxyServer().runtimeInfo();
    }

    public static ProxyEventsCaptured eventsCaptured() {
        return runningSimpleProxyServer().eventsCaptured();
    }

    static void currentProxyServer(SimpleProxyServer simpleProxyServer) {
        defaultInstance.set(new SimpleProxy(simpleProxyServer));
    }

    /* ============== helper methods ============== */
    public static Proxy buildHttpProxy() {
        return runningSimpleProxyServer().buildHttpProxy();
    }

    public static ProxySelector buildHttpProxySelector() {
        return runningSimpleProxyServer().buildHttpProxySelector();
    }

    public static InetSocketAddress buildHttpInetSocketAddress() {
        return runningSimpleProxyServer().buildHttpInetSocketAddress();
    }

    public static ProxySelector buildTlsProxySelector() {
        return runningSimpleProxyServer().buildTlsProxySelector();
    }

    public static InetSocketAddress buildTlsInetSocketAddress() {
        return runningSimpleProxyServer().buildTlsInetSocketAddress();
    }


    private static SimpleProxyServer runningSimpleProxyServer() {
        SimpleProxyServer simpleProxyServerInstance = defaultInstance.get().simpleProxyServer;
        if (simpleProxyServerInstance == null) {
            throw new IllegalStateException("Simple proxy server not started");
        }
        return simpleProxyServerInstance;
    }

}
