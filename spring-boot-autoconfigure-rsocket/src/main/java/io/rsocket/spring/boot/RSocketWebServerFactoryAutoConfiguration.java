/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.rsocket.spring.boot;

import java.util.Collection;

import io.rsocket.SocketAcceptor;
import reactor.netty.http.server.HttpServer;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.reactive.ReactiveWebServerFactoryAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.reactive.server.ReactiveWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.http.client.reactive.ReactorResourceFactory;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for a RSocket web server.
 *
 * @author Oleh Dokuka
 */
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@AutoConfigureBefore(ReactiveWebServerFactoryAutoConfiguration.class)
@Configuration
@ConditionalOnClass(ReactiveHttpInputMessage.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@EnableConfigurationProperties(RSocketServerProperties.class)
@Import({ RSocketWebServerFactoryAutoConfiguration.EmbeddedRSocket.class,})
public class RSocketWebServerFactoryAutoConfiguration {

	@Configuration
	@ConditionalOnMissingBean({ReactiveWebServerFactory.class})
	@ConditionalOnClass({HttpServer.class})
	static class EmbeddedRSocket {

		@Bean
		@ConditionalOnMissingBean
		public ReactorResourceFactory reactorServerResourceFactory() {
			return new ReactorResourceFactory();
		}

		@Bean
		public RSocketNettyReactiveWebServerFactory rSocketNettyReactiveWebServerFactory(
				ReactorResourceFactory resourceFactory,
				RSocketServerProperties rSocketServerProperties,
				SocketAcceptor socketAcceptor,
				Collection<RSocketReceiverCustomizer> customizers
		) {
			RSocketNettyReactiveWebServerFactory serverFactory = new RSocketNettyReactiveWebServerFactory();
			serverFactory.setResourceFactory(resourceFactory);
			serverFactory.setSocketAcceptor(socketAcceptor);
			serverFactory.setPath(rSocketServerProperties.getPath());
			serverFactory.setRSocketCustomizers(customizers);
			return serverFactory;
		}
	}
}
