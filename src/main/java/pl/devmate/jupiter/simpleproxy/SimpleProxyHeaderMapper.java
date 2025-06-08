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

import org.eclipse.jetty.http.HttpFields;
import pl.devmate.jupiter.simpleproxy.events.ProxyEventHttpHeader;
import pl.devmate.jupiter.simpleproxy.events.ProxyEventHttpHeaders;

import java.util.List;

class SimpleProxyHeaderMapper {

    private SimpleProxyHeaderMapper() {}

    static ProxyEventHttpHeaders toEventProxyHeaders(HttpFields headers) {
        if (headers == null) {
            return new ProxyEventHttpHeaders(List.of());
        }
        return new ProxyEventHttpHeaders(
                headers.stream()
                        .map(header -> new ProxyEventHttpHeader(header.getName(), header.getValueList()))
                        .toList()
        );
    }

}
