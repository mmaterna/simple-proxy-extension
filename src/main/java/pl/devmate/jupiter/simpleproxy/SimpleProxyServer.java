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

import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

class SimpleProxyServer {

    private static final Logger log = LoggerFactory.getLogger(SimpleProxyServer.class);

    private Server jettyServer = null;
    private final SimpleProxyConfig simpleProxyConfig;

    SimpleProxyServer(SimpleProxyConfig simpleProxyConfig) {
        this.simpleProxyConfig = simpleProxyConfig;
    }

    public void start() throws Exception {
        if (jettyServer != null) {
            throw new IllegalStateException("Jetty server already initialized");
        }
        if (simpleProxyConfig == null) {
            throw new IllegalArgumentException("simpleProxyConfig can't be null");
        }

        log.debug("Starting forward proxy server...");
        jettyServer = new Server(simpleProxyConfig.getPort());

        SimpleProxyHandler simpleProxyHandler = new SimpleProxyHandler();
        jettyServer.setHandler(simpleProxyHandler);
        jettyServer.start();
        log.debug("Started forward proxy server on port: {}", runningJettyPort());
    }

    public void stop() throws Exception {
        if (jettyServer != null && jettyServer.isRunning()) {
            jettyServer.stop();
        } else {
            log.debug("Server not running, no need to stop");
        }
    }

    public SimpleProxyContext context() {
        return new SimpleProxyContext(port().orElseThrow(() -> new IllegalStateException("Proxy server not started")));
    }

    private Optional<Integer> port() {
        if (jettyServer != null && jettyServer.isRunning()) {
            return Optional.of(runningJettyPort());
        }
        return Optional.empty();
    }

    private int runningJettyPort() {
        return jettyServer.getURI().getPort();
    }

}
