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

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public record ProxyEventsCaptured(
        List<ProxyEvent> proxyEvents
) {

    public ProxyEventsCaptured {
        proxyEvents = List.copyOf(proxyEvents);
    }

    public List<ProxyEvent> proxyEventsMatching(Predicate<ProxyEvent> filter) {
        return this.proxyEvents.stream()
                .filter(filter)
                .toList();
    }

    public List<ProxyEvent> proxyEventsWithUrlContaining(String uriPart) {
        return this.proxyEvents.stream()
                .filter(pe -> pe.clientToProxyRequest().uri().toString().toLowerCase().contains(uriPart.toLowerCase()))
                .toList();
    }

    public String summary() {
        String events = proxyEvents().stream()
                .map(this::shortInfo)
                .collect(Collectors.joining("\n\t", "\n\t", ""));
        return "Captured " + proxyEvents().size() + " proxy events:" + events;
    }

    private String shortInfo(ProxyEvent pe) {
        return "%s %s, %s".formatted(
                pe.clientToProxyRequest().method(),
                pe.clientToProxyRequest().uri().toString(),
                pe.serverToProxyResponse().httpStatusCode()
        );
    }

}
