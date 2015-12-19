package com.thoughtworks.go.communication.messages;

import com.thoughtworks.go.communication.Agent;
import com.thoughtworks.go.communication.Message;
import com.thoughtworks.go.communication.Messenger;
import com.thoughtworks.go.remote.work.Work;

public class WorkMessage implements Message<Agent> {
    private Work work;

    public WorkMessage(Work work) {
        this.work = work;
    }

    @Override
    public void execute(Messenger messenger, Agent host) {
        host.doWork(work);
    }

    @Override
    public String senderId() {
        return "server";
    }
}
