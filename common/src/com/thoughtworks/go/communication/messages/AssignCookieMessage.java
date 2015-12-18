package com.thoughtworks.go.communication.messages;

import com.thoughtworks.go.communication.Agent;
import com.thoughtworks.go.communication.Message;
import com.thoughtworks.go.communication.Messenger;

public class AssignCookieMessage implements Message<Agent> {
    private String cookie;

    public AssignCookieMessage(String cookie) {
        this.cookie = cookie;
    }

    @Override
    public void execute(Messenger messenger, Agent agent) {
        agent.setCookie(cookie);
    }

    @Override
    public String senderId() {
        return "server";
    }
}
