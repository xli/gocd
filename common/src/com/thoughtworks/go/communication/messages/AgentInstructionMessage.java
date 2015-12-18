package com.thoughtworks.go.communication.messages;

import com.thoughtworks.go.communication.Agent;
import com.thoughtworks.go.communication.Message;
import com.thoughtworks.go.communication.Messenger;
import com.thoughtworks.go.remote.AgentInstruction;

public class AgentInstructionMessage implements Message<Agent> {
    private AgentInstruction instruction;

    public AgentInstructionMessage(AgentInstruction instruction) {
        this.instruction = instruction;
    }

    @Override
    public void execute(Messenger messenger, Agent agent) {
        agent.setAgentInstruction(instruction);
    }

    @Override
    public String senderId() {
        return "server";
    }
}
