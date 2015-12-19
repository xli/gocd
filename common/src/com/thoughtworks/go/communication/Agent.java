package com.thoughtworks.go.communication;

import com.thoughtworks.go.remote.AgentInstruction;
import com.thoughtworks.go.remote.work.Work;

public interface Agent {
    void setCookie(String cookie);

    void setAgentInstruction(AgentInstruction instruction);

    void doWork(Work work);
}
