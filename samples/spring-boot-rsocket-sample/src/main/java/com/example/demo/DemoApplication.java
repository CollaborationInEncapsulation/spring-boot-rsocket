package com.example.demo;

import java.time.Duration;

import io.rsocket.AbstractRSocket;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.SocketAcceptor;
import io.rsocket.transport.netty.client.WebsocketClientTransport;
import io.rsocket.util.DefaultPayload;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.TcpClient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DemoApplication {
	private static final Log logger = LogFactory.getLog(DemoApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);

		RSocket rSocket = RSocketFactory.connect()
		                              .transport(WebsocketClientTransport.create(
				                              HttpClient.from(TcpClient.create()
				                                                       .host("localhost")
				                                                       .port(8080)),
				                              "/rsocket"
		                              ))
		                              .start()
		                              .block();

		logger.info(
			rSocket.requestResponse(DefaultPayload.create("HelloWorld"))
			     .map(Payload::getDataUtf8)
			     .block()
		);
	}

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

}

