package io.rsocket.spring.boot;

import io.rsocket.Closeable;
import io.rsocket.RSocketFactory;
import io.rsocket.SocketAcceptor;
import io.rsocket.transport.netty.server.CloseableChannel;
import io.rsocket.transport.netty.server.WebsocketRouteTransport;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.netty.ChannelBindException;
import reactor.netty.http.server.HttpServer;

import org.springframework.boot.web.embedded.netty.NettyWebServer;
import org.springframework.boot.web.server.PortInUseException;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.WebServerException;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.util.Assert;

public class RSocketWebServer implements WebServer {
    private static final Log logger = LogFactory.getLog(NettyWebServer.class);

    private final SocketAcceptor acceptor;

    private final String path;

    private final HttpServer httpServer;

    private final ReactorHttpHandlerAdapter handlerAdapter;

    private CloseableChannel disposableServer;

    public RSocketWebServer(HttpServer httpServer,
            ReactorHttpHandlerAdapter handlerAdapter,
            SocketAcceptor socketAcceptor,
            String path) {
        acceptor = socketAcceptor;
        this.path = path;
        Assert.notNull(httpServer, "HttpServer must not be null");
        Assert.notNull(handlerAdapter, "HandlerAdapter must not be null");
        this.httpServer = httpServer;
        this.handlerAdapter = handlerAdapter;
    }

    @Override
    public void start() throws WebServerException {
        if (this.disposableServer == null) {
            try {
                this.disposableServer = startHttpServer();
            }
            catch (Exception ex) {
                ChannelBindException bindException = findBindException(ex);
                if (bindException != null) {
                    throw new PortInUseException(bindException.localPort());
                }
                throw new WebServerException("Unable to start Netty", ex);
            }
            logger.info("Netty started on port(s): " + getPort());
            startDaemonAwaitThread(this.disposableServer);
        }
    }

    private CloseableChannel startHttpServer() {
        return RSocketFactory.receive()
                             .acceptor(acceptor)
                             .transport(new WebsocketRouteTransport(
                                 this.httpServer,
                                 r -> r.route(hsr -> !("/" + hsr.path()).equals(path), handlerAdapter),
                                 path
                             ))
                             .start()
                             .cast(CloseableChannel.class)
                             .block();
    }

    private ChannelBindException findBindException(Exception ex) {
        Throwable candidate = ex;
        while (candidate != null) {
            if (candidate instanceof ChannelBindException) {
                return (ChannelBindException) candidate;
            }
            candidate = candidate.getCause();
        }
        return null;
    }

    private void startDaemonAwaitThread(Closeable disposableServer) {
        Thread awaitThread = new Thread("server") {

            @Override
            public void run() {
                disposableServer.onClose().block();
            }

        };
        awaitThread.setContextClassLoader(getClass().getClassLoader());
        awaitThread.setDaemon(false);
        awaitThread.start();
    }

    @Override
    public void stop() throws WebServerException {
        if (this.disposableServer != null) {
            this.disposableServer.dispose();
            this.disposableServer = null;
        }
    }

    @Override
    public int getPort() {
        if (this.disposableServer != null) {
            return this.disposableServer.address().getPort();
        }
        return 0;
    }
}
