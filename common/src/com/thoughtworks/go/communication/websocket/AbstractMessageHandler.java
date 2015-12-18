package com.thoughtworks.go.communication.websocket;

import com.thoughtworks.go.communication.Message;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.io.*;

public abstract class AbstractMessageHandler<T> extends BinaryWebSocketHandler {

    protected abstract void handleMessage(WebSocketSession session, Message<T> message);

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        handleMessage(session, readMessage(message));
    }

    protected Message<T> readMessage(BinaryMessage message) throws IOException, ClassNotFoundException {
        ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(message.getPayload().array()));
        return (Message<T>) input.readObject();
    }

    protected BinaryMessage writeMessage(Message message) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ObjectOutputStream output = new ObjectOutputStream(out);
            output.writeObject(message);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new BinaryMessage(out.toByteArray());
    }

}
