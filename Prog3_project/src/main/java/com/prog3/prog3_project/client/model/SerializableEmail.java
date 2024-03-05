package com.prog3.prog3_project.client.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class SerializableEmail implements Serializable {

    private final String sender;
    private final String receiver;
    private final String subject;
    private final String body;

    private final Email.EmailState state;
    private final LocalDateTime dateTime;
    private final long ID;
    private final boolean read;

    public boolean isRead() {
        return read;
    }

    public long getID() {
        return ID;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public String getSender() {
        return sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public Email.EmailState getState() {
        return state;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }
    public SerializableEmail(Email email) {
        this.sender = email.getSender().strip();
        this.receiver = email.getReceiver().strip();
        this.subject = email.getSubject();
        this.body = email.getBody();
        this.state = email.getState();
        this.dateTime = email.getDateTime();
        this.ID = email.getID();
        this.read = email.isRead();
    }



}
