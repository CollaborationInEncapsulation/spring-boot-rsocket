package io.rsocket.spring.boot;

import java.util.function.Function;

import io.rsocket.RSocketFactory;

public interface RSocketReceiverCustomizer extends Function<RSocketFactory.ServerRSocketFactory, RSocketFactory.ServerRSocketFactory> {

}
