package com.thoughtworks.go.communication;

import com.thoughtworks.go.server.service.AgentRuntimeInfo;

public interface Server {
    void updateAgentRuntimeInfo(AgentRuntimeInfo info, Messenger messenger);
}
