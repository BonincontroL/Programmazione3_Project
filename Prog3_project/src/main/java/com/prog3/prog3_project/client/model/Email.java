package com.prog3.prog3_project.client.model;


import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Email implements Comparable<Email> {

    public enum EmailState {
        RECEIVED,
        SENT,
    }

    //identifico un'e-mail in base al mittente e a un ID
    private final StringProperty sender;// Rappresenta il mittente dell'email
    private final StringProperty receiver;//Rappresenta il destinatario dell'email
    private final StringProperty subject;//Rappresenta l'oggetto dell'email
    private final StringProperty body;//Rappresenta il corpo dell'email

    private long ID;
    private EmailState state;//Stato dell'email
    private final LocalDateTime dateTime;
    private boolean read = true;

    public long getID() {
        return ID;
    }

    public void setID(long id) {
        ID = id;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean value) {
        this.read = value;
    }

    public String getSender() {
        return sender.get();
    }

    public void setSender(String sender) {
        this.sender.set(sender);
    }

    public void setReceiver(String receiver) {
        this.receiver.set(receiver);
    }

    public StringProperty senderProperty() {
        return sender;
    }

    public String getReceiver() {
        return receiver.get();
    }

    public StringProperty receiverProperty() {
        return receiver;
    }

    public String getSubject() {
        return subject.get();
    }

    public StringProperty subjectProperty() {
        return subject;
    }

    public String getBody() {
        return body.get();
    }

    public StringProperty bodyProperty() {
        return body;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }


    public EmailState getState() {
        return state;
    }

    public void setState(EmailState state) {
        this.state = state;
    }

    public void setSubject(String subject) {
        this.subject.setValue(subject);
    }

    public void setBody(String body) {
        this.body.set(body);
    }

    public Email(String sender, String receiver, String subject, String body, EmailState state, LocalDateTime dateTime, long ID) {
        this.sender = new SimpleStringProperty(sender);
        this.receiver = new SimpleStringProperty(receiver);
        this.subject = new SimpleStringProperty(subject);
        this.body = new SimpleStringProperty(body);
        this.state = state;
        this.dateTime = dateTime;
        this.ID = ID;
    }

    public Email(SerializableEmail email) {
        this.sender = new SimpleStringProperty(email.getSender());
        this.receiver = new SimpleStringProperty(email.getReceiver());
        this.subject = new SimpleStringProperty(email.getSubject());
        this.body = new SimpleStringProperty(email.getBody());
        this.state = email.getState();
        this.dateTime = email.getDateTime();
        ID = email.getID();
        read = email.isRead();
    }

    public Email(long ID) {
        this.sender = new SimpleStringProperty("");
        this.receiver = new SimpleStringProperty("");
        this.subject = new SimpleStringProperty("");
        this.body = new SimpleStringProperty("");
        dateTime = LocalDateTime.now();
        this.ID = ID;
    }

    public Email clone() {
        return new Email(this.getSender(), this.getReceiver(), this.getSubject(), this.getBody(), this.getState(), this.getDateTime(), this.getID());
    }

    private static final Pattern VALID_EMAIL_ADDRESS_REGEX = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);

    public static boolean validateEmailAddress(String emailStr) {
        Matcher matcher = VALID_EMAIL_ADDRESS_REGEX.matcher(emailStr);
        return matcher.find();
    }


    @Override
    public String toString() {
        DateTimeFormatter ft = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        return "From: " + getSender() + "\n" +
                "To: " + getReceiver() + "\n" +
                "Subject: " + getSubject() + '\n' +
                "Data: " + ft.format(this.dateTime);
    }

    @Override
    public int compareTo(Email o) {
        if (sender.getValue().compareTo(o.getSender()) != 0) return sender.getValue().compareTo(o.getSender());
        return Long.compare(getID(), o.getID());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Email email = (Email) o;
        return ID == email.ID && (sender.getValue().compareTo(email.getSender()) == 0);
    }


}
