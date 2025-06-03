# SimpleProxy

Junit extension that starts http proxy. It can be useful for integration tests that require checking http communication using proxy.
It adds "simple-proxy-visited=true" header to response headers, so you can easily check if communication went through proxy.

## Examples

### SimpleProxyTest annotation

```java
import org.junit.jupiter.api.Test;
import pl.devmate.jupiter.simpleproxy.SimpleProxy;

import static org.assertj.core.api.Assertions.assertThat;

@SimpleProxyTest // starts forward proxy on random port before all tests
class ExampleTest {

    @Test
    void checkProxy() {
        // get proxy configuration
        Proxy proxy = SimpleProxy.context().getProxy();

        // ... call endpoints using proxy

        // http response should contain header SimpleProxy.SIMPLE_PROXY_VISITED_HEADER
        assertThat(response.headers().firstValue(SimpleProxy.SIMPLE_PROXY_VISITED_HEADER)).hasValue("true");
    }
}
```

### SimpleProxyExtension

```java
@ExtendWith({SimpleProxyExtension.class})
class ExampleTest {

    @Test
    void staticAccessToProxyContext() {
        Proxy proxy = SimpleProxy.context().getProxy();
        // ...
    }

    @Test
    void proxyContextAsParameter(SimpleProxyContext simpleProxyContext) {
        Proxy proxy = SimpleProxy.context().getProxy();
        // ...
    }
    
}
```

### Customized SimpleProxyExtension

```java

class ExampleTest {

    static SimpleProxyExtension proxyExtension = SimpleProxyExtension.builder().port(3128).build();
    
    @Test
    void customProxyExtension() {
        Proxy proxy = proxyExtension.context().getProxy();
        // ...
    }

}
```