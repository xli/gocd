package com.thoughtworks.go.communication;

import java.io.Serializable;

public interface Message<T> extends Serializable {
    void execute(Messenger messenger, T host);
    String senderId();
}
