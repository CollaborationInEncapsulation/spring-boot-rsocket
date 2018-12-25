package com.example.demo;

import java.time.Duration;

import io.netty.buffer.ByteBuf;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.stereotype.Service;

@Service
public class DefaultGreeterService implements Greeter {

    @Override
    public Mono<HelloResponse> requestGreet(HelloRequest message, ByteBuf metadata) {
        return Mono.just(HelloResponse.newBuilder()
                                      .setMessage("Greetings to : " + message.getName())
                                      .build());
    }

    @Override
    public Flux<HelloResponse> streamGreet(HelloRequest message, ByteBuf metadata) {
        return Flux
                .range(0, 100)
                .delayElements(Duration.ofMillis(500))
                .map(i -> HelloResponse.newBuilder()
                                       .setMessage("Greetings[" + i + "] to : " + message.getName())
                                       .build());
    }

    @Override
    public Flux<HelloResponse> channelGreet(Publisher<HelloRequest> messages, ByteBuf metadata) {
        return Flux
                .from(messages)
                .map(r -> HelloResponse.newBuilder()
                                       .setMessage("Greetings to : " + r.getName())
                                       .build());
    }
}
