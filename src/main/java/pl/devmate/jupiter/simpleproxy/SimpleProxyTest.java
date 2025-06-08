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


import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Registers JUnit Jupiter extension {@link SimpleProxyExtension} that starts
 * local proxy server.
 * Access to extension instance is available by injecting method parameter
 * <pre>
 * {@code
 * public void myTest(SimpleProxyExtension simpleProxyExtension) {
 *    int localProxyHttpPort = simpleProxyExtension.runtimeInfo().httpPort();
 *    Proxy proxy = simpleProxyExtension.getHttpProxy();
 *
 *    // ... call http endpoints using proxy
 *
 *    ProxyEventsCaptured events = simpleProxyExtension.eventsCaptured();
 *
 *    // ... check captured events
 * }
 * }
 * </pre>
 *
 * or using static access:
 *
 * <pre>
 * {@code
 * public void myTest() {
 *    int localProxyHttpPort = SimpleProxy.runtimeInfo().httpPort();
 *    Proxy proxy = SimpleProxy.getHttpProxy();
 *
 *    // ... call http endpoints using proxy
 *
 *    ProxyEventsCaptured events = SimpleProxy.eventsCaptured();
 *
 *    // ... check captured events
 * }
 * }
 * </pre>
 *
 * By default, proxy starts on random port, and add response header
 * {@value SimpleProxy#SIMPLE_PROXY_VISITED_HEADER}.
 * For request and response body capture use {@link #storeRequestBody()}
 * and {@link #storeResponseBody()} parameters.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(SimpleProxyExtension.class)
public @interface SimpleProxyTest {

    int httpPort() default 0;
    boolean addProxyResponseHeader() default true;
    boolean storeRequestBody() default false;
    boolean storeResponseBody() default false;

}
