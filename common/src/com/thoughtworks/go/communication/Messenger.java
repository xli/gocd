package com.thoughtworks.go.communication;

public interface Messenger {
    /**
     * send message
     * @param message
     * @return send message success of failed
     */
    boolean send(Message message);
}
