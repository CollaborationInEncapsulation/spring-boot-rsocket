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

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import io.rsocket.SocketAcceptor;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.server.HttpServer;
import reactor.netty.resources.LoopResources;

import org.springframework.boot.web.embedded.netty.NettyServerCustomizer;
import org.springframework.boot.web.embedded.netty.NettyWebServer;
import org.springframework.boot.web.embedded.netty.SslServerCustomizer;
import org.springframework.boot.web.reactive.server.AbstractReactiveWebServerFactory;
import org.springframework.boot.web.reactive.server.ReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServer;
import org.springframework.http.client.reactive.ReactorResourceFactory;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.util.Assert;

/**
 * {@link ReactiveWebServerFactory} that can be used to create {@link NettyWebServer}s.
 *
 * @author Oleh Dokuka
 */
public class RSocketNettyReactiveWebServerFactory extends AbstractReactiveWebServerFactory {

	private List<NettyServerCustomizer> serverCustomizers = new ArrayList<>();

	private boolean useForwardHeaders;

	private ReactorResourceFactory resourceFactory;

	private String path = "/rs";

	private SocketAcceptor socketAcceptor;

	public RSocketNettyReactiveWebServerFactory() {
	}

	public RSocketNettyReactiveWebServerFactory(int port) {
		super(port);
	}

	@Override
	public WebServer getWebServer(HttpHandler httpHandler) {
		HttpServer httpServer = createHttpServer();
		ReactorHttpHandlerAdapter handlerAdapter = new ReactorHttpHandlerAdapter(httpHandler);
		return new RSocketWebServer(httpServer, handlerAdapter, socketAcceptor, path);
	}

	/**
	 * Returns a mutable collection of the {@link NettyServerCustomizer}s that will be
	 * applied to the Netty server builder.
	 * @return the customizers that will be applied
	 */
	public Collection<NettyServerCustomizer> getServerCustomizers() {
		return this.serverCustomizers;
	}

	/**
	 * Set {@link NettyServerCustomizer}s that should be applied to the Netty server
	 * builder. Calling this method will replace any existing customizers.
	 * @param serverCustomizers the customizers to set
	 */
	public void setServerCustomizers(
			Collection<? extends NettyServerCustomizer> serverCustomizers) {
		Assert.notNull(serverCustomizers, "ServerCustomizers must not be null");
		this.serverCustomizers = new ArrayList<>(serverCustomizers);
	}

	/**
	 * Add {@link NettyServerCustomizer}s that should applied while building the server.
	 * @param serverCustomizers the customizers to add
	 */
	public void addServerCustomizers(NettyServerCustomizer... serverCustomizers) {
		Assert.notNull(serverCustomizers, "ServerCustomizer must not be null");
		this.serverCustomizers.addAll(Arrays.asList(serverCustomizers));
	}

	/**
	 * Set if x-forward-* headers should be processed.
	 * @param useForwardHeaders if x-forward headers should be used
	 * @since 2.1.0
	 */
	public void setUseForwardHeaders(boolean useForwardHeaders) {
		this.useForwardHeaders = useForwardHeaders;
	}

	/**
	 * Add {@link SocketAcceptor}s that should handle incoming RSocket clients.
	 * @param socketAcceptor the socket acceptor
	 */
	public void setSocketAcceptor(SocketAcceptor socketAcceptor) {
		Assert.notNull(socketAcceptor, "SocketAcceptor must not be null");
		this.socketAcceptor = socketAcceptor;
	}

	/**
	 * Set path on which RSocket server can observe incoming connections
	 */
	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * Set the {@link ReactorResourceFactory} to get the shared resources from.
	 * @param resourceFactory the server resources
	 * @since 2.1.0
	 */
	public void setResourceFactory(ReactorResourceFactory resourceFactory) {
		this.resourceFactory = resourceFactory;
	}

	private HttpServer createHttpServer() {
		HttpServer server = HttpServer.create();
		if (this.resourceFactory != null) {
			LoopResources resources = this.resourceFactory.getLoopResources();
			Assert.notNull(resources,
					"No LoopResources: is ReactorResourceFactory not initialized yet?");
			server = server.tcpConfiguration((tcpServer) -> tcpServer.runOn(resources)
					.addressSupplier(this::getListenAddress));
		}
		else {
			server = server.tcpConfiguration(
					(tcpServer) -> tcpServer.addressSupplier(this::getListenAddress));
		}
		if (getSsl() != null && getSsl().isEnabled()) {
			SslServerCustomizer sslServerCustomizer = new SslServerCustomizer(getSsl(),
					getHttp2(), getSslStoreProvider());
			server = sslServerCustomizer.apply(server);
		}
		if (getCompression() != null && getCompression().getEnabled()) {
			CompressionCustomizer compressionCustomizer = new CompressionCustomizer(
					getCompression());
			server = compressionCustomizer.apply(server);
		}
		server = server.protocol(listProtocols()).forwarded(this.useForwardHeaders);
		return applyCustomizers(server);
	}

	private HttpProtocol[] listProtocols() {
		if (getHttp2() != null && getHttp2().isEnabled()) {
			if (getSsl() != null && getSsl().isEnabled()) {
				return new HttpProtocol[] { HttpProtocol.H2, HttpProtocol.HTTP11 };
			}
			else {
				return new HttpProtocol[] { HttpProtocol.H2C, HttpProtocol.HTTP11 };
			}
		}
		return new HttpProtocol[] { HttpProtocol.HTTP11 };
	}

	private InetSocketAddress getListenAddress() {
		if (getAddress() != null) {
			return new InetSocketAddress(getAddress().getHostAddress(), getPort());
		}
		return new InetSocketAddress(getPort());
	}

	private HttpServer applyCustomizers(HttpServer server) {
		for (NettyServerCustomizer customizer : this.serverCustomizers) {
			server = customizer.apply(server);
		}
		return server;
	}

}
