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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import java.io.IOException;
import java.net.ProxySelector;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.util.stream.Collectors.joining;

class TestHelper {

    private static final Logger log = LoggerFactory.getLogger(TestHelper.class);
    static final String MOCKED_PATH = "/example-path";
    public static final String MOCKED_GET_RESPONSE_BODY = "Response from WireMock (GET)";
    public static final String MOCKED_POST_RESPONSE_BODY = "Response from WireMock (POST)";

    static void mockHttpServerGetResponse() {
        stubFor(get(urlEqualTo(MOCKED_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(MOCKED_GET_RESPONSE_BODY)
                        .withHeader("Content-Type", "text/plain")));
    }

    static void mockHttpServerPostResponse() {
        stubFor(post(urlEqualTo(MOCKED_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(MOCKED_POST_RESPONSE_BODY)
                        .withHeader("Content-Type", "text/plain")));
    }

    static HttpResponse<String> sendGetRequestWithProxy(ProxySelector proxySelector, String endpointUrl) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder().proxy(proxySelector).build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpointUrl))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        logResponse(response);
        return response;
    }

    static HttpResponse<String> sendPostRequestWithProxy(ProxySelector proxySelector, String endpointUrl, String requestBody) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder().proxy(proxySelector).build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpointUrl))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        logResponse(response);
        return response;
    }

    private static void logResponse(HttpResponse<String> response) {
        String headers = response.headers().map().entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(joining("\n\t"));
        log.debug("Response code: {},\nheaders:\n\t{},\nbody:\n{}", response.statusCode(), headers, response.body());
    }

    public static SSLContext prepareAcceptAllSslContext() throws NoSuchAlgorithmException, KeyManagementException {
        var trustManager = new X509ExtendedTrustManager() {

            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
                // accept all
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {
                // accept all
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }

            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) {
                // accept all
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) {
                // accept all
            }

            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
                // accept all
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
                // accept all
            }
        };
        var sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[]{trustManager}, new SecureRandom());
        return sslContext;
    }

}
