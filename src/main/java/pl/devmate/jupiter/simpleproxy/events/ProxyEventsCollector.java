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
package pl.devmate.jupiter.simpleproxy.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public class ProxyEventsCollector {

    private static final Logger log = LoggerFactory.getLogger(ProxyEventsCollector.class);
    private final List<ProxyEvent> proxyEvents = new CopyOnWriteArrayList<>();

    private final ProxyEventsCollectorConfig proxyEventsCollectorConfig;

    public ProxyEventsCollector(ProxyEventsCollectorConfig proxyEventsCollectorConfig) {
        Objects.requireNonNull(proxyEventsCollectorConfig, "proxyEventsCollectorConfig cannot be null");
        this.proxyEventsCollectorConfig = proxyEventsCollectorConfig;
    }

    public void register(ProxyEvent proxyEvent) {
        log.trace("Registering proxy event, uri: {}, response code: {}", proxyEvent.clientToProxyRequest().uri(), proxyEvent.serverToProxyResponse().httpStatusCode());
        this.proxyEvents.add(proxyEvent);
    }

    public void reset() {
        this.proxyEvents.clear();
    }

    public ProxyEventsCollectorConfig config() {
        return this.proxyEventsCollectorConfig;
    }

    public ProxyEventsCaptured eventsCaptured() {
        return new ProxyEventsCaptured(proxyEvents);
    }

}
