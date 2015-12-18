package com.thoughtworks.go.communication;

import java.io.*;

public class BinaryMessageHandler {
    public static Message readMessage(byte[] data) throws Exception {
        ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(data));
        return (Message) input.readObject();
    }

    private byte[] writeMessage(Message message) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream output = new ObjectOutputStream(out);
        output.writeObject(message);
        return out.toByteArray();
    }
}
