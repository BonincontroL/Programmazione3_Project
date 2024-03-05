package com.prog3.prog3_project.server.model;

import com.prog3.prog3_project.client.model.Client;

import java.io.Serializable;


public class Action implements Serializable {

    public enum Operation
    {
        //tutte le possibili operazioni che un client potrebbe richiedere al server
        SEND_EMAIL,
        DELETE_EMAIL,
        GET_ALL_EMAILS,
        READ_EMAIL
    }
    String sender;
    String receiver;
    Operation operation;
    boolean successful;
    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    public Action(Client sender, String receiverAddress, Operation operation) {
        this.sender = sender.getAddress();
        this.receiver = receiverAddress;
        this.operation = operation;
    }
    public String getSender() {
        return sender;
    }

    public Operation getOperation() {
        return operation;
    }

    public String getReceiver() {
        return receiver;
    }


    //report azioni client-server
    @Override
    public String toString() {
        return successful
                ? ">> OK: Action server -> Sender='" + sender + "', Receiver='" + receiver + "', Operation=" + operation
                : ">> Error!!!!!!!!!!!!!!!: Action server -> Sender='" + sender + "', Receiver='" + receiver + "', Operation=" + operation;
    }






}
