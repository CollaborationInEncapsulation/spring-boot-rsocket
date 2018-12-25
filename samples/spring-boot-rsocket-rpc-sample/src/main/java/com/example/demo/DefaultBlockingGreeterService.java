package com.example.demo;

import java.util.List;

import io.netty.buffer.ByteBuf;
import reactor.core.publisher.Flux;

import org.springframework.stereotype.Service;

@Service
public class DefaultBlockingGreeterService implements BlockingGreeter {

    @Override
    public HelloResponse requestGreet(HelloRequest message, ByteBuf metadata) {
        return HelloResponse.newBuilder()
                            .setMessage("Greetings to : " + message.getName())
                            .build();
    }

    @Override
    public List<HelloResponse> streamGreet(HelloRequest message, ByteBuf metadata) {
        return Flux
                .range(0, 100)
                .map(i -> HelloResponse.newBuilder()
                                       .setMessage("Greetings[" + i + "] to : " + message.getName())
                                       .build())
                .collectList()
                .block();
    }

    @Override
    public Iterable<HelloResponse> channelGreet(Iterable<HelloRequest> messages,
            ByteBuf metadata) {
        return Flux
                .fromIterable(messages)
                .map(r -> HelloResponse.newBuilder()
                                       .setMessage("Greetings to : " + r.getName())
                                       .build())
                .toIterable();
    }
}
