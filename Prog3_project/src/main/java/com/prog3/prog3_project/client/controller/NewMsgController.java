package com.prog3.prog3_project.client.controller;

import com.prog3.prog3_project.client.model.Client;
import com.prog3.prog3_project.client.model.Email;
import com.prog3.prog3_project.client.model.SerializableEmail;
import com.prog3.prog3_project.server.controller.ServerController;
import com.prog3.prog3_project.server.model.Action;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;


public class NewMsgController {

    @FXML
    private TextField textFieldTo;

    @FXML
    private TextField textFieldSubject;

    @FXML
    private TextArea textAreaMsg;


    // utilizzate per accedere ai dati del client e alle funzionalità del controller.
    private Client client;
    private ClientController clientController;

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public void setClientController(ClientController clientController) {
        this.clientController = clientController;
    }

    public void bindMsg() {
        textFieldTo.textProperty().bindBidirectional(client.newEmail.receiverProperty());
        textFieldSubject.textProperty().bindBidirectional(client.newEmail.subjectProperty());
        textAreaMsg.textProperty().bindBidirectional(client.newEmail.bodyProperty());
    }

    @FXML
    public void onSendButtonClicked(Event event) {
        doNewMailOperation(event, new Action(client, client.newEmail.getReceiver(), Action.Operation.SEND_EMAIL));
    }

    private void doNewMailOperation(Event event, Action action) {
        AtomicBoolean allGood = new AtomicBoolean(false);

        Thread t1 = new Thread(() -> {
            synchronized (clientController.reentrantLock) {
                try {
                    // Validazione del formato dell'email prima di stabilire la connessione
                    if (action.getOperation() == Action.Operation.SEND_EMAIL) {
                        String[] receiversTmp = action.getReceiver().split(",");

                        for (String s : receiversTmp) {
                            if (!Email.validateEmailAddress(s.trim())) {
                                Platform.runLater(() -> {
                                    Alert a = new Alert(Alert.AlertType.ERROR, "E-mail con formato errato");
                                    a.show();
                                });
                                return;
                            }
                        }
                    }

                    // Connessione e invio dei dati
                    clientController.getNewSocket();
                    clientController.setSocketSuccess();
                    clientController.sendActionToServer(action);
                    clientController.sendEmailToServer(new SerializableEmail(client.newEmail));
                    ServerController.ServerResponse response = clientController.waitForResponse();

                    // Gestione della risposta del server
                    if (response == ServerController.ServerResponse.ACTION_COMPLETED) {
                        allGood.set(true);
                    } else {
                        handleServerResponse(response);
                    }
                } catch (IOException socketException) {
                    clientController.setSocketFailure();
                } catch (ClassNotFoundException e) {
                    System.out.println("Impossibile leggere");
                } finally {
                    clientController.closeConnectionToServer();
                }
            }
        });
        t1.start();
        try {
            t1.join();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        // Gestione del successo dell'operazione
        if (allGood.get()) {
            resetNewEmailFields();
            clientController.loadAllFromServer();
            closeCurrentWindow(event);
        }
    }

    private void handleServerResponse(ServerController.ServerResponse response) {
        Platform.runLater(() -> {
            Alert alert;
            switch (response) {
                case RECEIVER_NOT_FOUND:
                    alert = new Alert(Alert.AlertType.ERROR, "Uno o più client a cui hai provato a inviare un'e-mail non sono stati trovati!");
                    break;
                default:
                    alert = new Alert(Alert.AlertType.ERROR, "Errore nell'invio email");
                    break;
            }
            alert.show();
        });
    }

    private void resetNewEmailFields() {
        //si reimpostare i campi dell'oggetto Email in modo che possa essere utilizzato per comporre una nuova email senza mantenere i valori della precedente.
        client.newEmail = new Email(client.getID() + 1);
        textAreaMsg.textProperty().unbindBidirectional(client.newEmail.bodyProperty());
        textFieldTo.textProperty().unbindBidirectional(client.newEmail.receiverProperty());
        textFieldSubject.textProperty().unbindBidirectional(client.newEmail.subjectProperty());
    }

    private void closeCurrentWindow(Event event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

}

