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

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.Request.Content;
import org.eclipse.jetty.client.Response.CompleteListener;
import org.eclipse.jetty.client.transport.HttpClientConnectionFactory;
import org.eclipse.jetty.client.transport.HttpClientTransportDynamic;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.client.transport.ClientConnectionFactoryOverHTTP2;
import org.eclipse.jetty.io.ClientConnectionFactory;
import org.eclipse.jetty.io.ClientConnector;
import org.eclipse.jetty.proxy.ProxyHandler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.devmate.jupiter.simpleproxy.events.ProxyEventsCollector;
import pl.devmate.jupiter.simpleproxy.events.ProxyEventsCollectorConfig;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

class SimpleProxyHandler extends ProxyHandler.Forward {

    private static final Logger log = LoggerFactory.getLogger(SimpleProxyHandler.class);
    public static final String SIMPLE_PROXY_REQUEST_CONTENT = "simple-proxy-request-content";
    public static final String SIMPLE_PROXY_RESPONSE_LISTENER = "simple-proxy-response-listener";
    public static final String SIMPLE_PROXY_REQUEST_TO_SERVER = "simple-proxy-request-to-server";
    public static final String SIMPLE_PROXY_RESPONSE_FROM_SERVER = "simple-proxy-response-from-server";

    private final ProxyEventsCollector proxyEventsCollector;

    SimpleProxyHandler(ProxyEventsCollector proxyEventsCollector) {
        this.proxyEventsCollector = proxyEventsCollector;
    }

    @Override
    protected Content newProxyToServerRequestContent(Request clientToProxyRequest, Response proxyToClientResponse, org.eclipse.jetty.client.Request proxyToServerRequest) {
        BufferingProxyRequestContent bufferingProxyToServerRequestContent = new BufferingProxyRequestContent(clientToProxyRequest, proxyEventsCollector.config());
        clientToProxyRequest.setAttribute(SIMPLE_PROXY_REQUEST_CONTENT, bufferingProxyToServerRequestContent);
        return bufferingProxyToServerRequestContent;
    }

    @Override
    protected CompleteListener newServerToProxyResponseListener(Request clientToProxyRequest, org.eclipse.jetty.client.Request proxyToServerRequest, Response proxyToClientResponse, Callback proxyToClientCallback) {
        BufferingProxyResponseListener bufferingServerToProxyResponseListener =
                new BufferingProxyResponseListener(clientToProxyRequest, proxyToServerRequest, proxyToClientResponse, proxyToClientCallback);
        clientToProxyRequest.setAttribute(SIMPLE_PROXY_RESPONSE_LISTENER, bufferingServerToProxyResponseListener);
        return bufferingServerToProxyResponseListener;
    }

    @Override
    protected void onProxyToClientResponseComplete(
            Request clientToProxyRequest,
            org.eclipse.jetty.client.Request proxyToServerRequest,
            org.eclipse.jetty.client.Response serverToProxyResponse,
            Response proxyToClientResponse,
            Callback proxyToClientCallback) {

        // store communication to server as attribute, to fill event data in connect handler
        clientToProxyRequest.setAttribute(SIMPLE_PROXY_REQUEST_TO_SERVER, proxyToServerRequest);
        clientToProxyRequest.setAttribute(SIMPLE_PROXY_RESPONSE_FROM_SERVER, serverToProxyResponse);

        // ---- call super class implementation
        super.onProxyToClientResponseComplete(clientToProxyRequest, proxyToServerRequest, serverToProxyResponse, proxyToClientResponse, proxyToClientCallback);
    }


    @Override
    protected HttpClient newHttpClient() {
        QueuedThreadPool proxyClientThreads = new QueuedThreadPool();
        proxyClientThreads.setName("simple-proxy-client");
        ClientConnector proxyClientConnector = new ClientConnector();
        proxyClientConnector.setSelectors(1);
        proxyClientConnector.setExecutor(proxyClientThreads);
        proxyClientConnector.setSslContextFactory(new SslContextFactory.Client(true));

        HTTP2Client proxyHTTP2Client = new HTTP2Client(proxyClientConnector);
        ClientConnectionFactory.Info h1 = HttpClientConnectionFactory.HTTP11;
        ClientConnectionFactory.Info http2 = new ClientConnectionFactoryOverHTTP2.HTTP2(proxyHTTP2Client);
        return new HttpClient(new HttpClientTransportDynamic(proxyClientConnector, h1, http2));
    }

    /**
     * Buffer request content in ByteArrayOutputStream, if enabled in configuration
     */
    protected static class BufferingProxyRequestContent extends ProxyRequestContent {

        private final ProxyEventsCollectorConfig proxyEventsCollectorConfig;
        private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        public BufferingProxyRequestContent(Request clientToProxyRequest, ProxyEventsCollectorConfig proxyEventsCollectorConfig) {
            super(clientToProxyRequest);
            this.proxyEventsCollectorConfig = proxyEventsCollectorConfig;
        }

        @Override
        public org.eclipse.jetty.io.Content.Chunk read() {
            org.eclipse.jetty.io.Content.Chunk chunk = super.read();

            try {
                if (proxyEventsCollectorConfig.storeRequestBody()) {
                    baos.write(BufferUtil.toArray(chunk.getByteBuffer().duplicate()));
                }
            } catch (Exception e) {
                log.debug("Could not store request body content", e);
            }

            return chunk;
        }

        protected byte[] requestContent() {
            return baos.toByteArray();
        }
    }

    /**
     * Listener with the ability to buffer response body
     */
    protected class BufferingProxyResponseListener extends ProxyResponseListener {

        private final ByteArrayOutputStream responseBytesOutputStream = new ByteArrayOutputStream();

        public BufferingProxyResponseListener(Request clientToProxyRequest, org.eclipse.jetty.client.Request proxyToServerRequest, Response proxyToClientResponse, Callback proxyToClientCallback) {
            super(clientToProxyRequest, proxyToServerRequest, proxyToClientResponse, proxyToClientCallback);
        }

        @Override
        public void onContent(org.eclipse.jetty.client.Response serverToProxyResponse, org.eclipse.jetty.io.Content.Chunk serverToProxyChunk, Runnable serverToProxyDemander) {
            ByteBuffer serverToProxyContent = serverToProxyChunk.getByteBuffer();
            if (proxyEventsCollector.config().storeRequestBody()) {
                try {
                    responseBytesOutputStream.write(BufferUtil.toArray(serverToProxyContent.duplicate()));
                } catch (Exception e) {
                    log.debug("Could not store response body content", e);
                }
            }

            // call base implementation
            super.onContent(serverToProxyResponse, serverToProxyChunk, serverToProxyDemander);
        }

        protected byte[] responseContent() {
            return responseBytesOutputStream.toByteArray();
        }

    }

}
