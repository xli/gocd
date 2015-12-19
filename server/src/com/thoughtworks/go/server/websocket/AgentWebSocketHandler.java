package com.thoughtworks.go.server.websocket;

import com.thoughtworks.go.communication.Message;
import com.thoughtworks.go.communication.Messenger;
import com.thoughtworks.go.communication.Server;
import com.thoughtworks.go.communication.messages.AgentInstructionMessage;
import com.thoughtworks.go.communication.messages.AssignCookieMessage;
import com.thoughtworks.go.communication.messages.WorkMessage;
import com.thoughtworks.go.communication.websocket.AbstractMessageHandler;
import com.thoughtworks.go.remote.AgentInstruction;
import com.thoughtworks.go.remote.BuildRepositoryRemote;
import com.thoughtworks.go.remote.work.Work;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.server.service.BuildAssignmentService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AgentWebSocketHandler extends AbstractMessageHandler<Server> implements Server {
    private static final Logger LOGGER = Logger.getLogger(AgentWebSocketHandler.class);
    private static final String MESSAGE_SENDER_ID = "message.sender.id";


    private Map<String, WebSocketSession> agentSessions = new ConcurrentHashMap();
    private BuildRepositoryRemote buildRepositoryRemote;
    private BuildAssignmentService buildAssignmentService;

    @Autowired
    public AgentWebSocketHandler(BuildRepositoryRemote buildRepositoryRemote, BuildAssignmentService buildAssignmentService) {
        this.buildRepositoryRemote = buildRepositoryRemote;
        this.buildAssignmentService = buildAssignmentService;
        this.buildAssignmentService.setAgentWebSocketHandler(this);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String agentUUId = (String) session.getAttributes().get(MESSAGE_SENDER_ID);
        LOGGER.info("Connection closed for agent " + agentUUId + ", close status: " + status);
        lostAgent(agentUUId);
    }

    @Override
    protected void handleMessage(WebSocketSession session, Message<Server> message) {
        registerAgent(message.senderId(), session);
        message.execute(createMessager(session), this);
    }

    @Override
    public void updateAgentRuntimeInfo(AgentRuntimeInfo info, Messenger messenger) {
        if (info.getCookie() == null) {
            String cookie = buildRepositoryRemote.getCookie(info.getIdentifier(), info.getLocation());
            info.setCookie(cookie);
            if (!messenger.send(new AssignCookieMessage(cookie))) {
                return;
            }
        }
        AgentInstruction instruction = buildRepositoryRemote.ping(info);
        //TODO: should be sent when server got cancel message, instead of waiting for agent runtime info message :)
        messenger.send(new AgentInstructionMessage(instruction));
    }

    protected Messenger createMessager(final WebSocketSession session) {
        return new Messenger() {
            @Override
            public boolean send(Message message) {
                try {
                    session.sendMessage(writeMessage(message));
                    return true;
                } catch (IOException e) {
                    LOGGER.info("Unexpected error with message sender, probably connection dropped", e);
                    lostAgent(message.senderId());
                    return false;
                }
            }
        };
    }

    private void registerAgent(String agentUUId, WebSocketSession session) {
        if (agentUUId.equals(session.getAttributes().get(MESSAGE_SENDER_ID)) || !agentSessions.containsKey(agentUUId)) {
            LOGGER.info("Register WebSocket session for agent " + agentUUId);
            session.getAttributes().put(MESSAGE_SENDER_ID, agentUUId);
            agentSessions.putIfAbsent(agentUUId, session);
        }
    }

    private void lostAgent(String agentUUId) {
        LOGGER.info("Remove WebSocket session for agent " + agentUUId);
        agentSessions.remove(agentUUId);
    }

    public Collection<String> connectedAgentUUIds() {
        return agentSessions.keySet();
    }

    public void assignWork(String agentUUId, Work work) {
        createMessager(agentSessions.get(agentUUId)).send(new WorkMessage(work));
    }
}
