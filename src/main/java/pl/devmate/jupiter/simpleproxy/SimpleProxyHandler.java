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

import org.eclipse.jetty.proxy.ProxyHandler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

class SimpleProxyHandler extends ProxyHandler.Forward {

    @Override
    public boolean handle(Request clientToProxyRequest, Response proxyToClientResponse, Callback proxyToClientCallback) {
        proxyToClientResponse.getHeaders().add(SimpleProxy.SIMPLE_PROXY_VISITED_HEADER, "true");
        return super.handle(clientToProxyRequest, proxyToClientResponse, proxyToClientCallback);
    }

}
