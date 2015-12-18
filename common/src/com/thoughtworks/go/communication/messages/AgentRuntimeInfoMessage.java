package com.thoughtworks.go.communication.messages;

import com.thoughtworks.go.communication.Message;
import com.thoughtworks.go.communication.Messenger;
import com.thoughtworks.go.communication.Server;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;

public class AgentRuntimeInfoMessage implements Message<Server> {

    private AgentRuntimeInfo info;

    public AgentRuntimeInfoMessage(AgentRuntimeInfo info) {
        this.info = info;
    }

    @Override
    public void execute(Messenger messenger, Server server) {
        server.updateAgentRuntimeInfo(info, messenger);
    }

    @Override
    public String senderId() {
        return info.getUUId();
    }
}
