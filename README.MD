# Spring Boot RSocket Starter

This project is a draft of Spring Boot integration with RSocket.

At the moment, this starter support simples integration with WebFlux and RSocket.
Using this starter you can easily create WebFlux server and WebSocket RSocket Receiver 
on the same port.

# Quick Start

Copy the following dependencies to the project 

```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation ('com.github.CollaborationInEncapsulation.spring-boot-rsocket:spring-boot-starter-rsocket:0.0.1.RELEASE')
}
```

Provide `SocketAcceptor` bean to the project


```java
@Bean
public SocketAcceptor socketAcceptor() {
    return ((setup, sendingSocket) -> Mono.just(new AbstractRSocket() {
        @Override
        public Mono<Void> fireAndForget(Payload payload) {
            logger.info("Handled fnf with payload: [" + payload + "]");
            return Mono.empty();
        }

        @Override
        public Mono<Payload> requestResponse(Payload payload) {
            logger.info("Handled requestResponse with payload: [" + payload + "]");
            return Mono.just(DefaultPayload.create("Echo: " + payload.getDataUtf8()));
        }

        @Override
        public Flux<Payload> requestStream(Payload payload) {
            logger.info("Handled requestStream with payload: [" + payload + "]");
            return Flux
                    .interval(Duration.ofSeconds(1))
                    .map(i -> DefaultPayload.create(
                            "Echo[" + i + "]:" + payload.getDataUtf8()
                    ));
        }

        @Override
        public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
            logger.info("Handled requestChannel");
            return Flux
                    .from(payloads)
                    .map(payload -> DefaultPayload.create(
                            "Echo:" + payload.getDataUtf8()
                    ));
        }
    }));
}
```

Run your application and check it using RSocket client 

```java
RSocket rSocket = RSocketFactory
    .connect()
    .transport(WebsocketClientTransport.create(
        HttpClient.from(TcpClient.create()
                                 .host("localhost")
                                 .port(8080)),
        "/rs"
    ))
    .start()
    .block();

logger.info(
    rSocket.requestResponse(DefaultPayload.create("HelloWorld"))
          .map(Payload::getDataUtf8)
          .block()
);
```

# Available configurations

Since current Spring Boot RSocket integration is an addon to WebFlux module, you can 
configure `HttpServer` using properties provided by Spring WebFlux

In addition, it is available option of RSocket handling path configuration:

```yaml
rsocket:
    server:
        path: /rsocket
``` 

# Bugs and Feedback
For bugs, questions and discussions please use the [Github Issues](https://github.com/CollaborationInEncapsulation/spring-boot-rsocket/issues/new).

# LICENSE
Copyright 2018 the original author or authors.

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.