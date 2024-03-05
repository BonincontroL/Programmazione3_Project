package com.prog3.prog3_project.client.model;

import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;


public class Client {

    private final SimpleStringProperty address;//Una stringa che rappresenta l'indirizzo email del cliente.
    private final SimpleListProperty<Email> inbox;//Una lista di oggetti Email rappresentanti la casella di posta in arrivo del cliente.
    private final SimpleListProperty<Email> sent;//Una lista di oggetti Email rappresentanti le email inviate dal cliente.

    private final SimpleStringProperty status;
    public Email selectedEmail; //rappresenta l'email selezionata
    public Email newEmail;//rappresenta una nuova email.

    public Client(String address) {
        this.address = new SimpleStringProperty(address);
        this.inbox = new SimpleListProperty<>(FXCollections.observableArrayList());//una lista osservabile
        this.sent = new SimpleListProperty<>(FXCollections.observableArrayList());
        this.status = new SimpleStringProperty("");
    }

    public SimpleStringProperty addressProperty() {
        return address;
    }

    public SimpleListProperty<Email> inboxProperty() {
        return inbox;
    }

    public SimpleStringProperty statusProperty() {
        return status;
    }

    public String getAddress() {
        return address.get();
    }

    public SimpleListProperty<Email> sentProperty() {
        return sent;
    }

    /**
     * @param email l'email da cercare.
     * @return SimpleListProperty dove si "trova" l'e-mail passata dal param
     */
    public SimpleListProperty<Email> whereIs(Email email) {
        SimpleListProperty<Email> ret = null;
        if (inboxProperty().contains(email))
            ret = inboxProperty();
        if (sentProperty().contains(email))
            ret = sentProperty();
        return ret;
    }

    /**
     * Metodo che controlla ogni email del cliente
     * e restituisce il suo ID massimo
     *
     * @return ID massimo
     */
    public long getID() {
        long max = 0;
        SimpleListProperty<Email> allEmails = new SimpleListProperty<>(FXCollections.observableArrayList());
        allEmails.addAll(inboxProperty());
        allEmails.addAll(sentProperty());
        for (Email email : allEmails) {
            if (email.getID() > max) max = email.getID();
        }
        return max;
    }

    /**
     * @param emailList emailList in cui controlliamo la posta
     * @param id        ID utilizzato per cercare nell'elenco dell'e-mail
     * @return Email e con l'id passato dal param
     */
    public Email findEmailById(SimpleListProperty<Email> emailList, long id) {
        for (Email e : emailList) {
            if (e.getID() == id)
                return e;
        }
        return null;
    }

    /**
     * Metodo utilizzato per cercare nella sezione se
     * c'è un'altra email con lo stesso ID dell'email passata
     * per parametro
     *
     * @param list  Lista email
     * @param email Email da cercare nella sezione
     * @return true se esiste già
     */
    public boolean hasSameIDInCollection(SimpleListProperty<Email> list, Email email) {
        for (Email emailIterated : list) {
            if (emailIterated.getID() == email.getID()) {
                return true;
            }
        }
        return false;
    }


}
