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

import static java.util.Objects.requireNonNullElseGet;

public class SimpleProxyExtension implements BeforeAllCallback, AfterAllCallback, ParameterResolver {

    private static final Logger log = LoggerFactory.getLogger(SimpleProxyExtension.class);

    private SimpleProxyServer simpleProxyServer;
    private final SimpleProxyConfig config;

    public SimpleProxyExtension() {
        config = new SimpleProxyConfig();
    }

    public SimpleProxyExtension(SimpleProxyConfig config) {
        this.config = requireNonNullElseGet(config, SimpleProxyConfig::new);
    }

    /**
     * Create instance with default proxy server configuration (proxy on a random port)
     */
    public static SimpleProxyExtension create() {
        return new SimpleProxyExtension();
    }

    public static SimpleProxyExtensionBuilder builder() {
        return new SimpleProxyExtensionBuilder();
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
        return parameterContext.getParameter().getType().equals(SimpleProxyContext.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        if (parameterContext.getParameter().getType().equals(SimpleProxyContext.class)) {
            return simpleProxyServer.context();
        }
        return null;
    }

    public SimpleProxyContext context() {
        if (simpleProxyServer == null) {
            throw new IllegalStateException("Simple proxy server not running");
        }
        return simpleProxyServer.context();
    }

    private SimpleProxyConfig prepareConfiguration(ExtensionContext extensionContext) {
        return extensionContext
                .getElement()
                .flatMap(element -> AnnotationSupport.findAnnotation(element, SimpleProxyTest.class))
                .map(this::buildConfigFromAnnotation)
                .orElse(this.config);
    }

    private SimpleProxyConfig buildConfigFromAnnotation(SimpleProxyTest simpleProxyTest) {
        SimpleProxyConfig simpleProxyConfig = new SimpleProxyConfig();
        simpleProxyConfig.setPort(simpleProxyTest.httpPort());
        return simpleProxyConfig;
    }

}
