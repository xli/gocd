package com.thoughtworks.go.agent.service;

import com.thoughtworks.go.communication.Agent;
import com.thoughtworks.go.communication.Message;
import com.thoughtworks.go.communication.Messenger;
import com.thoughtworks.go.communication.websocket.AbstractMessageHandler;
import com.thoughtworks.go.util.URLService;
import org.apache.log4j.Logger;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.jetty.JettyWebSocketClient;
import org.springframework.web.socket.handler.LoggingWebSocketHandlerDecorator;

import javax.net.ssl.SSLContext;
import java.io.IOException;

@Service
public class WebSocketService extends AbstractMessageHandler<Agent> implements Messenger {
    private static final Logger LOGGER = Logger.getLogger(WebSocketService.class);
    private URLService urlService;
    private JettyWebSocketClient client;
    private Agent host;
    private WebSocketSession session;

    @Autowired
    public WebSocketService(URLService urlService) {
        this.urlService = urlService;
    }

    public void setHost(Agent agent) {
        this.host = agent;
    }

    public synchronized boolean isRunning() {
        return client != null &&
                client.isRunning() &&
                this.session != null &&
                this.session.isOpen();
    }

    public synchronized void start(SSLContext sslContext) throws Exception {
        if (client != null) {
            client.stop();
        }
        SslContextFactory factory = new SslContextFactory(false);
        factory.setSslContext(sslContext);
        client = new JettyWebSocketClient(new org.eclipse.jetty.websocket.client.WebSocketClient(factory));
        client.start();
        if (this.session != null && this.session.isOpen()) {
            this.session.close();
        }
        this.session = client.doHandshake(new LoggingWebSocketHandlerDecorator(this), this.urlService.getAgentWebSocketUrl()).get();
    }

    private synchronized WebSocketSession getSession() {
        return session;
    }

    @Override
    public boolean send(Message message){
        WebSocketSession session = getSession();
        if (session == null) {
            return false;
        }
        try {
            session.sendMessage(writeMessage(message));
            return true;
        } catch (IOException e) {
            LOGGER.info("Unexpected error when send message, probably connection dropped", e);
            return false;
        }
    }

    @Override
    protected void handleMessage(WebSocketSession session, Message<Agent> message) {
        message.execute(this, host);
    }

}
