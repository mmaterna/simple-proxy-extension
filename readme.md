# SimpleProxy

Junit extension, which helps in writing integration tests for functionalities using http proxy.<br>
Allows you to start a local http proxy and check whether network traffic passed through the proxy.

## Examples

See tests for example usage.

## Usage

Register JUnit extension to start proxy server, and capture requests.

### Maven

Add library as test dependency

```xml
<dependency>
    <groupId>pl.devmate.jupiter.extensions.simpleproxy</groupId>
    <artifactId>simple-proxy-extension</artifactId>
    <version>1.0.1</version>
    <scope>test</scope>
</dependency>
```

### Register extension

#### Register extension using SimpleProxyTest annotation

```java
import org.junit.jupiter.api.Test;
import pl.devmate.jupiter.simpleproxy.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SimpleProxyTest // starts forward proxy on random port before all tests
class ExampleTest {

    @Test
    void checkProxy() throws Exception {
        // get proxy configuration
        HttpClient client = HttpClient.newBuilder().proxy(SimpleProxy.buildHttpProxySelector()).build();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create("http://example.com")).GET().build();

        // send request through proxy
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // http response should contain header SimpleProxy.SIMPLE_PROXY_VISITED_HEADER
        assertThat(response.headers().firstValue(SimpleProxy.SIMPLE_PROXY_VISITED_HEADER)).hasValue("true");
    }
}
```

#### Register extension using "ExtendWith" annotation

```java
import pl.devmate.jupiter.simpleproxy.*;

@ExtendWith({SimpleProxyExtension.class})
class ExampleTest {

    @Test
    void staticAccessToProxyContext() {
        Proxy proxy = SimpleProxy.buildHttpProxy();
        // ...
    }

    @Test
    void proxyContextAsParameter(SimpleProxyExtension simpleProxyExtension) {
        Proxy proxy = simpleProxyExtension.buildHttpProxy();
        // ...
    }

}
```

#### Register extension using RegisterExtension annotation

```java
import pl.devmate.jupiter.simpleproxy.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class ExampleTest {

    @RegisterExtension
    static SimpleProxyExtension simpleProxyExtension = new SimpleProxyExtension(SimpleProxyConfig.builder()
            .port(0)
            .addProxyResponseHeader(true)
            .storeRequestBody(true)
            .storeResponseBody(true)
            .build()
    );
    
    @Test
    void check() {
        Proxy proxy = simpleProxyExtension.buildHttpProxy();
        // ...
    }

}
```

### Get captured data

Requests that go through proxy are captured and can be inspected.

```java
import pl.devmate.jupiter.simpleproxy.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;

@SimpleProxyTest(storeRequestBody = true, storeResponseBody = true)
class ExampleWithCaptureTest {

    @Test
    void checkExample(SimpleProxyExtension simpleProxyExtension) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .proxy(simpleProxyExtension.buildHttpProxySelector())
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://example.com"))
                .GET()
                .build();

        // send request through proxy
        client.send(request, HttpResponse.BodyHandlers.ofString());

        // --- check captured events
        ProxyEventsCaptured events = simpleProxyExtension.eventsCaptured();

        assertThat(events.proxyEvents()).hasSize(1);
        ProxyEvent proxyEvent = events.proxyEvents().get(0);
        assertThat(proxyEvent.clientToProxyRequest().method()).isEqualTo("GET");
        assertThat(proxyEvent.clientToProxyRequest().requestBytes()).isNull();
        assertThat(proxyEvent.serverToProxyResponse().responseBytes()).isNotEmpty();
        assertThat(proxyEvent.serverToProxyResponse().httpStatusCode()).isEqualTo(200);
    }
}

```
