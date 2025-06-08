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

import org.junit.jupiter.api.extension.*;
import org.junit.platform.commons.support.AnnotationSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.devmate.jupiter.simpleproxy.events.ProxyEventsCaptured;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.util.Objects;

/**
 * JUnit Jupiter extension that starts local proxy server.<br>
 * By default, proxy starts on random port, and add response header
 * {@value SimpleProxy#SIMPLE_PROXY_VISITED_HEADER}.
 * Requests sent through proxy are captured, so you can later verify whether the request was sent using the proxy.
 * <br>
 * The easiest way to register an extension is to use annotation {@link SimpleProxyTest}.<br>
 * If you need more control, you can register the extension using JUnit annotation:
 *
 * <pre>
 * {@code
 * import org.junit.jupiter.api.extension.RegisterExtension;
 *
 * class SimpleHttpProxyExtensionTest {
 *
 *     @RegisterExtension
 *     static SimpleProxyExtension simpleProxyExtension = new SimpleProxyExtension(SimpleProxyConfig.builder()
 *         .port(0)
 *         .addProxyResponseHeader(true)
 *         .storeRequestBody(true)
 *         .storeResponseBody(true)
 *         .build()
 *     );
 *
 *     @Test
 *     void injectedContextAndStaticValuesShouldBeEqual() {
 *         int localProxyHttpPort = simpleProxyExtension.runtimeInfo().httpPort();
 *         Proxy proxy = simpleProxyExtension.getHttpProxy();
 *
 *         // ... call http endpoints using proxy
 *
 *         ProxyEventsCaptured events = simpleProxyExtension.eventsCaptured();
 *
 *         // ... check captured events
 *     }
 *
 * }
 * }
 * </pre> *
 */
public class SimpleProxyExtension implements BeforeAllCallback, AfterAllCallback, ParameterResolver, BeforeEachCallback, AfterEachCallback {

    private static final Logger log = LoggerFactory.getLogger(SimpleProxyExtension.class);
    public static final String LOCALHOST = "localhost";

    private SimpleProxyServer simpleProxyServer;
    private final SimpleProxyConfig config;

    public SimpleProxyExtension() {
        config = SimpleProxyConfig.DEFAULT;
    }

    public SimpleProxyExtension(SimpleProxyConfig config) {
        Objects.requireNonNull(config, "config cannot be null");
        this.config = config;
    }

    /**
     * Create instance with default proxy server configuration (proxy on a random port)
     */
    public static SimpleProxyExtension create() {
        return new SimpleProxyExtension();
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        if (simpleProxyServer != null) {
            throw new IllegalStateException("SimpleProxyServer already created");
        }
        SimpleProxyConfig proxyServerConfiguration = prepareConfiguration(context);
        simpleProxyServer = new SimpleProxyServer(proxyServerConfiguration);
        simpleProxyServer.start();
        SimpleProxy.currentProxyServer(simpleProxyServer);
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        if (simpleProxyServer != null) {
            log.debug("Stopping proxy server...");
            simpleProxyServer.stop();
            log.debug("Stopped proxy server");
        } else {
            log.debug("Simple proxy server not running, nothing to stop");
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        Class<?> parameterClass = parameterContext.getParameter().getType();
        return parameterClass.equals(SimpleProxyExtension.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        if (parameterContext.getParameter().getType().equals(SimpleProxyExtension.class)) {
            return this;
        }
        return null;
    }

    public SimpleProxyRuntimeInfo runtimeInfo() {
        if (simpleProxyServer == null) {
            throw new IllegalStateException("Simple proxy server not running");
        }
        return simpleProxyServer.runtimeInfo();
    }

    public ProxyEventsCaptured eventsCaptured() {
        return simpleProxyServer.eventsCaptured();
    }

    private SimpleProxyConfig prepareConfiguration(ExtensionContext extensionContext) {
        return extensionContext
                .getElement()
                .flatMap(element -> AnnotationSupport.findAnnotation(element, SimpleProxyTest.class))
                .map(this::buildConfigFromAnnotation)
                .orElse(this.config);
    }

    private SimpleProxyConfig buildConfigFromAnnotation(SimpleProxyTest simpleProxyTest) {
        return SimpleProxyConfig.builder()
                .port(simpleProxyTest.httpPort())
                .addProxyResponseHeader(simpleProxyTest.addProxyResponseHeader())
                .storeRequestBody(simpleProxyTest.storeRequestBody())
                .storeResponseBody(simpleProxyTest.storeResponseBody())
                .build();
    }

    @Override
    public void afterEach(ExtensionContext context) {
        // empty
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        simpleProxyServer.resetCapturedEvents();
    }

    /*
    Utility methods that create common proxy objects
     */
    public Proxy buildHttpProxy() {
        return runningSimpleProxyServer().buildHttpProxy();
    }

    public ProxySelector buildHttpProxySelector() {
        return runningSimpleProxyServer().buildHttpProxySelector();
    }

    public InetSocketAddress buildHttpInetSocketAddress() {
        return runningSimpleProxyServer().buildHttpInetSocketAddress();
    }

    public ProxySelector buildTlsProxySelector() {
        return runningSimpleProxyServer().buildTlsProxySelector();
    }

    public InetSocketAddress buildTlsInetSocketAddress() {
        return runningSimpleProxyServer().buildTlsInetSocketAddress();
    }

    private SimpleProxyServer runningSimpleProxyServer() {
        if (simpleProxyServer == null) {
            throw new IllegalStateException("Simple proxy server not started");
        }
        return simpleProxyServer;
    }
}
