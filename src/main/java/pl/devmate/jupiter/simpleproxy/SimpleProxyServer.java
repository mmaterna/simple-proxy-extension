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

import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http2.HTTP2Cipher;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.proxy.ProxyHandler;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.ConnectHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.devmate.jupiter.simpleproxy.events.ProxyEventsCaptured;
import pl.devmate.jupiter.simpleproxy.events.ProxyEventsCollector;
import pl.devmate.jupiter.simpleproxy.events.ProxyEventsCollectorConfig;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.util.Arrays;
import java.util.Optional;

import static pl.devmate.jupiter.simpleproxy.SimpleProxyExtension.LOCALHOST;

class SimpleProxyServer {

    private static final Logger log = LoggerFactory.getLogger(SimpleProxyServer.class);
    public static final String SIMPLE_PROXY_TLS_CONNECTOR_NAME = "simple-proxy-tls";
    public static final String SIMPLE_PROXY_HTTP_CONNECTOR_NAME = "simple-proxy-http";

    private Server jettyServer = null;
    private final SimpleProxyConfig simpleProxyConfig;
    private final ProxyEventsCollector proxyEventsCollector;

    SimpleProxyServer(SimpleProxyConfig simpleProxyConfig) {
        this.simpleProxyConfig = simpleProxyConfig;
        ProxyEventsCollectorConfig collectorConfig = new ProxyEventsCollectorConfig(
                simpleProxyConfig.storeRequestBody(),
                simpleProxyConfig.storeResponseBody()
        );
        this.proxyEventsCollector = new ProxyEventsCollector(collectorConfig);
    }

    public void start() throws Exception {
        if (jettyServer != null) {
            throw new IllegalStateException("Jetty server already initialized");
        }
        if (simpleProxyConfig == null) {
            throw new IllegalArgumentException("simpleProxyConfig can't be null");
        }

        log.debug("Starting forward proxy server...");
        ConnectHandler connectHandler = new SimpleProxyConnectHandler(proxyEventsCollector, simpleProxyConfig);
        SimpleProxyHandler proxyHandler = new SimpleProxyHandler(proxyEventsCollector);
        jettyServer = startProxy(connectHandler, proxyHandler);
        log.debug("Started forward proxy server, http port: {}, https port: {}",
                httpPort().map(Object::toString).orElse("<none>"),
                httpsPort().map(Object::toString).orElse("<none>"));
    }

    private Server startProxy(ConnectHandler connectHandler, ProxyHandler proxyHandler) throws Exception {
        SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
        sslContextFactory.setUseCipherSuitesOrder(true);
        sslContextFactory.setCipherComparator(HTTP2Cipher.COMPARATOR);

        QueuedThreadPool proxyThreads = new QueuedThreadPool(10);
        proxyThreads.setName("simple-proxy");
        var proxy = new Server(proxyThreads);

        HttpConfiguration httpConfig = new HttpConfiguration();
        
        ConnectionFactory h1c = new HttpConnectionFactory(httpConfig);
        ConnectionFactory h2c = new HTTP2CServerConnectionFactory(httpConfig);
        var proxyConnector = new ServerConnector(proxy, 1, 1, h1c, h2c);
        proxyConnector.setName(SIMPLE_PROXY_HTTP_CONNECTOR_NAME);
        proxy.addConnector(proxyConnector);

        HttpConfiguration httpsConfig = new HttpConfiguration(httpConfig);
        httpsConfig.addCustomizer(new SecureRequestCustomizer());
        HttpConnectionFactory h1 = new HttpConnectionFactory(httpsConfig);
        HTTP2ServerConnectionFactory h2 = new HTTP2ServerConnectionFactory(httpsConfig);

        ALPNServerConnectionFactory alpn = new ALPNServerConnectionFactory();
        alpn.setDefaultProtocol(h1.getProtocol());
        SslConnectionFactory ssl = new SslConnectionFactory(sslContextFactory, alpn.getProtocol());

        var proxyTLSConnector = new ServerConnector(proxy, 1, 1, ssl, alpn, h2, h1, h2c);
        proxyTLSConnector.setName(SIMPLE_PROXY_TLS_CONNECTOR_NAME);
        proxy.addConnector(proxyTLSConnector);
        proxy.setHandler(connectHandler);
        connectHandler.setHandler(proxyHandler);

        proxy.start();
        return proxy;
    }

    public void stop() throws Exception {
        if (jettyServer != null && jettyServer.isRunning()) {
            jettyServer.stop();
        } else {
            log.debug("Server not running, no need to stop");
        }
    }

    public SimpleProxyRuntimeInfo runtimeInfo() {
        return new SimpleProxyRuntimeInfo(
                httpPort().orElseThrow(() -> new IllegalStateException("Proxy server not started")),
                httpsPort().orElse(null)
                );
    }

    public ProxyEventsCaptured eventsCaptured() {
        return proxyEventsCollector.eventsCaptured();
    }

    public void resetCapturedEvents() {
        proxyEventsCollector.reset();
    }

    private Optional<Integer> httpPort() {
        if (jettyServer != null && jettyServer.isRunning()) {
            return Optional.of(runningJettyHttpPort());
        }
        return Optional.empty();
    }

    private Optional<Integer> httpsPort() {
        if (jettyServer != null && jettyServer.isRunning()) {
            return Optional.of(runningJettyTlsPort());
        }
        return Optional.empty();
    }

    private int runningJettyHttpPort() {
        return findProxyPortByConnectorName(SIMPLE_PROXY_HTTP_CONNECTOR_NAME)
                .orElseThrow(() -> new IllegalStateException("Could not find http proxy port"));
    }

    private int runningJettyTlsPort() {
        return findProxyPortByConnectorName(SIMPLE_PROXY_TLS_CONNECTOR_NAME)
                .orElseThrow(() -> new IllegalStateException("Could not find TLS proxy port"));
    }

    private Optional<Integer> findProxyPortByConnectorName(String connectorName) {
        if (jettyServer == null) {
            return Optional.empty();
        }
        return Arrays.stream(jettyServer.getConnectors())
                .filter(ServerConnector.class::isInstance)
                .map(ServerConnector.class::cast)
                .filter(serverConnector -> connectorName.equals(serverConnector.getName()))
                .map(ServerConnector::getLocalPort)
                .findAny();
    }

    /*
    Utility methods that create common proxy objects
     */
    public Proxy buildHttpProxy() {
        return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(LOCALHOST, runtimeInfo().httpPort()));
    }

    public ProxySelector buildHttpProxySelector() {
        return ProxySelector.of(buildHttpInetSocketAddress());
    }

    public InetSocketAddress buildHttpInetSocketAddress() {
        return new InetSocketAddress(LOCALHOST, runtimeInfo().httpPort());
    }

    public ProxySelector buildTlsProxySelector() {
        return ProxySelector.of(buildHttpInetSocketAddress());
    }

    public InetSocketAddress buildTlsInetSocketAddress() {
        return new InetSocketAddress(LOCALHOST, runtimeInfo().httpsPort());
    }

}
