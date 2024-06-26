package com.soroko.common;

import java.io.Serializable;

public class Message implements Serializable {
    private String sender;
    private String text;
    private String sentAt;
    private boolean FilesAreEmpty;

    public Message(String sender) {
        this.sender = sender;
    }

    public String getSender() {
        return sender;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getSentAt() {
        return sentAt;
    }

    public void setSentAt(String sentAt) {
        this.sentAt = sentAt;
    }

    public boolean getFilesAreEmpty() {
        return FilesAreEmpty;
    }

    public void setFilesAreEmpty(boolean filesAreEmpty) {
        FilesAreEmpty = filesAreEmpty;
    }
}
