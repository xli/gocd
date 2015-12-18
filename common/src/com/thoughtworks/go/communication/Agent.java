package com.thoughtworks.go.communication;

import com.thoughtworks.go.remote.AgentInstruction;

public interface Agent {
    void setCookie(String cookie);

    void setAgentInstruction(AgentInstruction instruction);
}
