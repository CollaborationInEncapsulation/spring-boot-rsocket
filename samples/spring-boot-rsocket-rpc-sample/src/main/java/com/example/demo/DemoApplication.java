package com.example.demo;

import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.SocketAcceptor;
import io.rsocket.rpc.rsocket.RequestHandlingRSocket;
import io.rsocket.transport.netty.client.WebsocketClientTransport;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.TcpClient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);

		RSocket rSocket = RSocketFactory.connect()
		                              .transport(WebsocketClientTransport.create(
				                              HttpClient.from(TcpClient.create()
				                                                       .host("localhost")
				                                                       .port(8080)),
				                              "/rsocket-rpc"
		                              ))
		                              .start()
		                              .block();

		GreeterClient client = new GreeterClient(rSocket);

		client.streamGreet(HelloRequest.newBuilder().setName("Jon Doe").build())
		      .log()
		      .blockLast();

		client.requestGreet(HelloRequest.newBuilder().setName("Arthur Conan Doyle").build())
		      .log()
		      .block();
	}

	@Bean
	public SocketAcceptor socketAcceptor(GreeterServer greeter,
			BlockingGreeterServer blockingGreeterServer) {
		return ((setup, sendingSocket) -> Mono.just(new RequestHandlingRSocket(
			greeter, blockingGreeterServer
		)));
	}

}

