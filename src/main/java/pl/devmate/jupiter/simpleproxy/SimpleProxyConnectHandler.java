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

import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.handler.ConnectHandler;
import org.eclipse.jetty.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.devmate.jupiter.simpleproxy.events.ProxyEvent;
import pl.devmate.jupiter.simpleproxy.events.ProxyEventRequest;
import pl.devmate.jupiter.simpleproxy.events.ProxyEventResponse;
import pl.devmate.jupiter.simpleproxy.events.ProxyEventsCollector;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static pl.devmate.jupiter.simpleproxy.SimpleProxyHandler.*;
import static pl.devmate.jupiter.simpleproxy.SimpleProxyHeaderMapper.toEventProxyHeaders;

class SimpleProxyConnectHandler extends ConnectHandler {

    private static final Logger log = LoggerFactory.getLogger(SimpleProxyConnectHandler.class);
    private final ProxyEventsCollector proxyEventsCollector;
    private final SimpleProxyConfig simpleProxyConfig;

    public SimpleProxyConnectHandler(ProxyEventsCollector proxyEventsCollector, SimpleProxyConfig simpleProxyConfig) {
        this.proxyEventsCollector = proxyEventsCollector;
        this.simpleProxyConfig = simpleProxyConfig;
    }

    @Override
    public boolean handle(Request request, Response response, Callback callback) throws Exception {
        if (HttpMethod.CONNECT.is(request.getMethod())) {
            log.trace("SimpleProxy will not capture events (requests) sent through connect tunnel. Only HTTP traffic is observed.");
        }
        if (simpleProxyConfig.addProxyResponseHeader()) {
            response.getHeaders().add(SimpleProxy.SIMPLE_PROXY_VISITED_HEADER, "true");
        }
        Callback simpleProxyConnectCallback = new SimpleProxyConnectCallback(request, response, callback);
        return super.handle(request, response, simpleProxyConnectCallback);
    }

    private class SimpleProxyConnectCallback implements Callback {
        private final Response response;
        private final Callback callback;
        private final Request request;

        public SimpleProxyConnectCallback(Request request, Response response, Callback callback) {
            this.response = response;
            this.callback = callback;
            this.request = request;
        }

        @Override
        public void completeWith(CompletableFuture<?> completable) {
            callback.completeWith(completable);
        }

        @Override
        public void succeeded() {
            log.trace("[succeeded] response code in callback: {}", response.getStatus());
            storeSimpleProxyEventInCollector(request, response);
            callback.succeeded();
        }

        @Override
        public void failed(Throwable x) {
            log.trace("[failed] response code in callback: {}", response.getStatus());
            storeSimpleProxyEventInCollector(request, response);
            callback.failed(x);
        }

        /**
         Called after response to client complete. Now collect proxy-event data:
         - request headers sent from client to proxy
         - request body captured when calling server (handling request body to server)
         - response headers returned from server to proxy
         - response body captured while handing server response (server response to proxy)
         */
        private void storeSimpleProxyEventInCollector(Request clientToProxyRequest, Response proxyToClientResponse) {
            try {
                // try to find server-to-proxy response stored as attribute (in SimpleProxyHandler)
                org.eclipse.jetty.client.Response serverToProxyResponse = storedServerToProxyResponse(clientToProxyRequest)
                        .orElse(null);

                // fill request body bytes if available
                byte[] clientToProxyRequestContentBytes = bufferingProxyRequestContentAttribute(clientToProxyRequest)
                        .map(SimpleProxyHandler.BufferingProxyRequestContent::requestContent)
                        .orElse(null);

                ProxyEventRequest finalClientToProxyRequestEvent = new ProxyEventRequest(
                        Instant.ofEpochMilli(Request.getTimeStamp(clientToProxyRequest)),
                        clientToProxyRequest.getHttpURI().toURI(),
                        clientToProxyRequest.getMethod(),
                        toEventProxyHeaders(clientToProxyRequest.getHeaders()),
                        clientToProxyRequestContentBytes
                );

                // fill response body bytes if available
                byte[] responseBodyBytes = bufferingProxyResponseListenerAttribute(clientToProxyRequest)
                        .map(SimpleProxyHandler.BufferingProxyResponseListener::responseContent)
                        .orElse(null);

                ProxyEventResponse serverToProxyResponseEvent;
                if (serverToProxyResponse != null) {
                    serverToProxyResponseEvent = new ProxyEventResponse(
                            Instant.now(),
                            serverToProxyResponse.getStatus(),
                            toEventProxyHeaders(serverToProxyResponse.getHeaders()),
                            responseBodyBytes
                    );
                } else {
                    // no attribute w server-to-proxy response, then fill data based on
                    // proxy-to-client response (e.g., for CONNECT requests)
                    serverToProxyResponseEvent = new ProxyEventResponse(
                            Instant.now(),
                            proxyToClientResponse.getStatus(),
                            toEventProxyHeaders(proxyToClientResponse.getHeaders()),
                            responseBodyBytes
                    );
                }

                // store proxy event in collector
                proxyEventsCollector.register(new ProxyEvent(
                        finalClientToProxyRequestEvent,
                        serverToProxyResponseEvent
                ));
            } catch (Exception e) {
                log.warn("Could not store simple proxy event in collector", e);
            }
            clearSimpleProxyAttributes(clientToProxyRequest);
        }

        private Optional<SimpleProxyHandler.BufferingProxyResponseListener> bufferingProxyResponseListenerAttribute(Request clientToProxyRequest) {
            Object attribute = clientToProxyRequest.getAttribute(SIMPLE_PROXY_RESPONSE_LISTENER);
            if (attribute instanceof SimpleProxyHandler.BufferingProxyResponseListener clientToProxyResponseListener) {
                return Optional.of(clientToProxyResponseListener);
            }
            return Optional.empty();
        }

        private Optional<SimpleProxyHandler.BufferingProxyRequestContent> bufferingProxyRequestContentAttribute(Request clientToProxyRequest) {
            Object attribute = clientToProxyRequest.getAttribute(SIMPLE_PROXY_REQUEST_CONTENT);
            if (attribute instanceof SimpleProxyHandler.BufferingProxyRequestContent clientToProxyRequestContent) {
                return Optional.of(clientToProxyRequestContent);
            }
            return Optional.empty();
        }

        private Optional<org.eclipse.jetty.client.Response> storedServerToProxyResponse(Request clientToProxyRequest) {
            Object attribute = clientToProxyRequest.getAttribute(SIMPLE_PROXY_RESPONSE_FROM_SERVER);
            if (attribute instanceof org.eclipse.jetty.client.Response serverToProxyResponse) {
                return Optional.of(serverToProxyResponse);
            }
            return Optional.empty();
        }

        private void clearSimpleProxyAttributes(Request clientToProxyRequest) {
            clientToProxyRequest.removeAttribute(SIMPLE_PROXY_RESPONSE_LISTENER);
            clientToProxyRequest.removeAttribute(SIMPLE_PROXY_REQUEST_CONTENT);
            clientToProxyRequest.removeAttribute(SIMPLE_PROXY_REQUEST_TO_SERVER);
            clientToProxyRequest.removeAttribute(SIMPLE_PROXY_RESPONSE_FROM_SERVER);
        }

    }

}
