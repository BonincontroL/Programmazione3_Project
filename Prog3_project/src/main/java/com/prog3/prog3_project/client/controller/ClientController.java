package com.prog3.prog3_project.client.controller;

import com.prog3.prog3_project.client.model.Client;
import com.prog3.prog3_project.client.model.Email;
import com.prog3.prog3_project.client.model.SerializableEmail;
import com.prog3.prog3_project.server.controller.ServerController;
import com.prog3.prog3_project.server.model.Action;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.Button;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class ClientController {


    // Dichiarazione degli elementi di interfaccia grafica (FXML)

    public Button restartConnButton;
    @FXML
    private Label dateValueLabel;
    @FXML
    private TextArea emailContentTextArea;
    @FXML
    private ListView<Email> folderListView;
    @FXML
    private Label userNameLabel;
    @FXML
    private Label fromValueLabel;
    @FXML
    private Label toValueLabel;
    @FXML
    private Label subjectValueLabel;
    @FXML
    private Label statusLabel;


    // Dichiarazione di variabili di istanza
    private Stage newMessageStage;//Rappresenta lo stage associato a una finestra  di nuovo messaggio
    private Client client;
    private Socket socket;
    private ScheduledExecutorService scheduledExEmailDownloader;
    protected final ReentrantLock reentrantLock = new ReentrantLock();
    ObjectOutputStream objectOutputStream;
    ObjectInputStream objectInputStream;

    // Metodo per impostare lo Stage associato alla finestra di nuovo messaggio
    public void setStage(Stage newMessageStage) {
        this.newMessageStage = newMessageStage;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        if (this.client != null)
            throw new IllegalStateException("Il client può essere inizializzato solo una volta");
        this.client = client;
    }

    //Associazione dell'etichetta di stato in basso a sinistra della GUI
    // che ci consente di vedere se il Client è connesso al Server
    public Label getStatusLabel() {
        return statusLabel;
    }

    public void setStatusBiding() {
        this.getStatusLabel().textProperty().bind(client.statusProperty());
    }//binding dell'etichetta di stato

    public Label getAccountLabel() {
        return userNameLabel;
    }

    public void bindClientProperties() {
        this.getAccountLabel().textProperty().bind(client.addressProperty());
    }

    public void handleDelete(ActionEvent actionEvent) {
        try {
            deleteSelectedEmail();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handleForward(ActionEvent actionEvent) {
        if (client.selectedEmail == null || client.selectedEmail.getSender().equals("")) {
            Platform.runLater(() -> {
                Alert a = new Alert(Alert.AlertType.ERROR, "Seleziona un'e-mail!!!!");
                a.show();
            });
            return;
        }
        try {
            if (client.selectedEmail != null) {
                client.newEmail = new Email(client.getID() + 1);
                client.newEmail.setSender(client.addressProperty().get());
                client.newEmail.setSubject(client.selectedEmail.getSubject());
                client.newEmail.setBody(client.selectedEmail.getBody());
                newMessageStage.show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void handleReply(ActionEvent actionEvent) { // Rispondi a tutti
        if (client.selectedEmail == null || client.selectedEmail.getSender().equals("")) {
            Platform.runLater(() -> {
                Alert a = new Alert(Alert.AlertType.ERROR, "Seleziona un'e-mail!!!!");
                a.show();
            });
            return;
        }
        try {
            if (client.selectedEmail != null) {
                client.newEmail = new Email(client.getID() + 1);
                client.newEmail.setSender(client.addressProperty().get());
                client.newEmail.setReceiver(client.selectedEmail.getSender());
                client.newEmail.setSubject("Re: " + client.selectedEmail.getSubject());
                newMessageStage.show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handleReplyAll(ActionEvent actionEvent) {
        if (client.selectedEmail == null || client.selectedEmail.getSender().equals("")) {
            Platform.runLater(() -> {
                Alert a = new Alert(Alert.AlertType.ERROR, "Seleziona un'e-mail!!!!");
                a.show();
            });
            return;
        }
        try {
            if (client.selectedEmail != null) {
                client.newEmail = new Email(client.getID() + 1);
                client.newEmail.setSender(client.addressProperty().get());
                String[] receivers = client.selectedEmail.getReceiver().split(",");
                StringBuilder parameter = new StringBuilder();
                for (int i = 0; i < receivers.length; i++) {
                    String receiver = receivers[i].strip();//rimuove spazi bianchi dall'inizio e dalla fine di una stringa.
                    if (client.getAddress().equals(receiver))
                        parameter.append(client.selectedEmail.getSender());
                    else
                        parameter.append(receiver);
                    if (i != receivers.length - 1)
                        parameter.append(",");
                }
                client.newEmail.setReceiver(parameter.toString());
                client.newEmail.setSubject("Re: " + client.selectedEmail.getSubject());
                newMessageStage.show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void bindMailToView(Email email) {
        try {
            Platform.runLater(() -> {

                fromValueLabel.textProperty().bind(email.senderProperty());
                toValueLabel.textProperty().bind(email.receiverProperty());
                subjectValueLabel.textProperty().bind(email.subjectProperty());
                emailContentTextArea.textProperty().bind(email.bodyProperty());

                LocalDateTime dateTime = email.getDateTime();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
                String formattedDateTime = dateTime.format(formatter);
                dateValueLabel.setText(formattedDateTime);

            });

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    /**
     * Metodo richiamato quando il Client fa clic sul pulsante nuovo Email,
     * newEmail è impostata su un new Email con getID+1,
     * Il nuovo mittente dell'e-mail è impostato sull'indirizzo del Client (mittente),
     * e infine mostriamo la nuova scena del messaggio
     */
    public void handleNewEmail() throws IOException {
        // Metodo per gestire la creazione di un nuovo messaggio
        try {
            client.newEmail = new Email(client.getID() + 1);
            client.newEmail.setSender(client.addressProperty().get());
            newMessageStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    /**
     * Metodo chiamato quando si fa clic su ListView (selezione di un'e-mail),
     * se ListView è vuoto, restituisce
     * altrimenti selezioniamo la prima Email e leghiamo questa alla vista.
     * Successivamente controlliamo se l'e-mail è già stata letta, se la risposta è falsa
     * creiamo un thread che contatta il server, invia l'azione, invia l'e-mail e
     * attendere che la risposta imposti il vero stato della variabile letta.
     *
     * @param event (MouseEvent) evento che chiama onListViewClick
     */
    @FXML
    private void onListViewClick(MouseEvent event) {

        if (folderListView.getSelectionModel().getSelectedItems().size() <= 0) {//controlla se è stata effettauata una selezione
            return;
        }

        Email email = folderListView.getSelectionModel().getSelectedItems().get(0);
        client.selectedEmail = email;
        bindMailToView(email);

        if (!email.isRead()) { //Verifica se l'email selezionata non è stata letta
            new Thread(() -> {
                try {
                    getNewSocket();
                    setSocketSuccess();
                    sendActionToServer(new Action(client, null, Action.Operation.READ_EMAIL));//Viene inviata al server un'azione di lettura dell'email
                    sendEmailToServer(new SerializableEmail(client.selectedEmail));
                    ServerController.ServerResponse response = waitForResponse();
                    if (response == ServerController.ServerResponse.ACTION_COMPLETED) {
                        email.setRead(true);
                    }
                } catch (IOException exception) {
                    setSocketFailure();
                } catch (ClassNotFoundException e) {
                    System.out.println("Impossibile leggere");
                } finally {
                    closeConnectionToServer();
                }
            }).start();

        }

    }

    /**
     * Il metodo di supporto resetSelectedEmail separa l'e-mail
     * selezionata dalla vista per crearne una nuova vuota e la associa alla
     * vista del Client.
     */
    private void resetSelectedEmail() {
        client.selectedEmail = new Email(client.getID() + 1);

        Platform.runLater(() -> {
            fromValueLabel.textProperty().unbind();
            toValueLabel.textProperty().unbind();
            subjectValueLabel.textProperty().unbind();
            emailContentTextArea.textProperty().unbind();

            bindMailToView(client.selectedEmail);
        });
    }


    /**
     * ShowNextEmail è un metodo che consente l'aggiornamento della vista del Cliente
     * l'e-mail selezionata automaticamente quando il Cliente elimina un'e-mail.
     * Per prima cosa controlliamo se è l'ultima email, se è vera, semplicemente slegamo l'email selezionata alla vista, ne creiamo una nuova vuota e leghiamo l'ultima alla vista del cliente
     * Altrimenti, controlla se l'indexOfDeletedEmail è 0 (il primo visualizzato),
     * se è vero chiamiamo resetSelectedEmail, quindi assegniamo a selectEmail quella successiva e la leghiamo alla vista del Cliente.
     * L'ultimo caso contiene tutti gli altri indici ed esegue la stessa operazione di indiceOfDeletedEmail == 0 ma quello successivo verrà selezionato utilizzando indice: indiceOfDeletedEmail-1.
     *
     * @param indexOfDeletedEmail dell'e-mail da eliminare
     */
    private void showNextEmail(int indexOfDeletedEmail) {
        if (folderListView.getItems().size() == 1) {
            resetSelectedEmail();
            return;
        }

        if (indexOfDeletedEmail == 0) {
            resetSelectedEmail();
            folderListView.getSelectionModel().select(1);
            client.selectedEmail = folderListView.getItems().get(1);
            bindMailToView(client.selectedEmail);
        }

        if (indexOfDeletedEmail > 0 && indexOfDeletedEmail - 1 < folderListView.getItems().size()) {
            resetSelectedEmail();
            folderListView.getSelectionModel().select(indexOfDeletedEmail - 1);
            client.selectedEmail = folderListView.getItems().get(indexOfDeletedEmail - 1);
            bindMailToView(client.selectedEmail);
        }
    }


    /**
     * Metodo richiamato quando un Client fa clic sul pulsante Elimina,
     * Per prima cosa controlliamo se l'e-mail selezionata non è vuota/null (vero: avviso)
     * se è false, creiamo Thread che gestisce una connessione con Server,
     * invia l'Azione (DELETE_EMAIL), invia l'E-mail da eliminare e attende una risposta.
     * Se la risposta è ACTION_COMPLETED troviamo l'indice delle Email, le scarichiamo
     * nuova e-mail dal server, quindi reimpostare la vista client per essere pronta a visualizzare quella nuova,
     * e infine chiamiamo showNextEmail che mostra l'e-mail successiva.
     */
    private void deleteSelectedEmail() {
        if (client.selectedEmail == null || client.selectedEmail.getSender().equals("")) {
            Platform.runLater(() -> {
                Alert a = new Alert(Alert.AlertType.ERROR, "Seleziona un'e-mail!!!!");
                a.show();
            });
            return;
        }
        new Thread(() -> {
            synchronized (reentrantLock) {
                ServerController.ServerResponse response = null;
                if (client.selectedEmail != null) {
                    try {
                        getNewSocket();
                        setSocketSuccess();
                        sendActionToServer(new Action(client, null, Action.Operation.DELETE_EMAIL));
                        SerializableEmail emailToBeDeleted = new SerializableEmail(client.selectedEmail);
                        sendEmailToServer(emailToBeDeleted);
                        response = waitForResponse();
                    } catch (IOException socketException) {
                        setSocketFailure();
                    } catch (ClassNotFoundException e) {
                        System.out.println("Impossibile leggere");
                    } finally {
                        closeConnectionToServer();
                    }
                    if (response == ServerController.ServerResponse.ACTION_COMPLETED) {
                        int indexOfEmail = client.whereIs(client.selectedEmail).indexOf(client.selectedEmail);
                        loadAllFromServer();
                        resetSelectedEmail();
                        showNextEmail(indexOfEmail);
                    } else {
                        Platform.runLater(() -> {
                            Alert a = new Alert(Alert.AlertType.ERROR, "Errore durante l'eliminazione di un'e-mail");
                            a.show();
                        });
                    }
                }
            }
        }).start();
    }

    /**
     * loadAllFromServer è il cuore di questo progetto, questo metodo scarica nuove email dal server e le aggiunge ai client.
     * Un Thread gestisce tutte le operazioni effettuate,utilizzate per prevenire il blocco della vista.
     * quindi il compito di Thread è: connettersi al server, inviare GET_ALL_EMAILS Azione, attendere la risposta del server, se la risposta è ACTION_COMPLETED,
     * creiamo un ArrayList che contiene tutte le email inviate dal server, quindi lo scorriamo e controlliamo ogni stato email e per ogni email controlliamo
     * se esiste già (non modificare o non nuova email) se è falsa aggiungiamo localmente l'email nuova/modificata.
     * L'ultima operazione è un ciclo che controlla se ci sono email che non sono sul server, ma lo sono ancora nel Client,
     * in quello scenario le email extra vengono eliminate.
     */
    @FXML
    public void loadAllFromServer() {
        // Metodo per caricare tutte le email dal server
        new Thread(() -> {
            synchronized (reentrantLock) {
                try {
                    getNewSocket();
                    setSocketSuccess();
                } catch (IOException e) {
                    setSocketFailure();
                    return;
                }
                Action request = new Action(client, null, Action.Operation.GET_ALL_EMAILS);
                sendActionToServer(request);
                ServerController.ServerResponse response = null;
                try {
                    response = waitForResponse();
                } catch (Exception ex) {
                    System.out.println("nessuna risposta in loadAllFromServer");
                }
                if (response == ServerController.ServerResponse.ACTION_COMPLETED) {
                    ArrayList<Email> emailsFromServer = new ArrayList<>();
                    try {
                        SerializableEmail serializableEmail;
                        while ((serializableEmail = (SerializableEmail) objectInputStream.readObject()) != null) {
                            Email serverEmail = new Email(serializableEmail);
                            emailsFromServer.add(serverEmail);
                        }
                    } catch (EOFException EOFException) {
                        System.out.println("Finito di ricevere le email");
                    } catch (ClassNotFoundException e) {
                        System.out.println("Impossibile leggere");
                    } catch (IOException ioException) {
                        System.out.println("Errore durante la lettura delle email in loadAllFromServer");
                    } finally {
                        closeConnectionToServer();
                    }

                    AtomicInteger newMails = new AtomicInteger();
                    for (Email serverEmail : emailsFromServer) {
                        switch (serverEmail.getState()) {
                            case RECEIVED -> {
                                if (!client.hasSameIDInCollection(client.inboxProperty(), serverEmail)) {
                                    if (!serverEmail.isRead()) {
                                        newMails.getAndIncrement();
                                    }
                                    Platform.runLater(() -> client.inboxProperty().add(serverEmail));
                                }
                            }
                            case SENT -> {
                                if (!client.hasSameIDInCollection(client.sentProperty(), serverEmail))
                                    Platform.runLater(() -> client.sentProperty().add(serverEmail));
                            }
                        }
                    }
                    // Ordina la lista per data
                    Platform.runLater(() -> {
                        client.inboxProperty().sort((email1, email2) ->
                                email2.getDateTime().compareTo(email1.getDateTime()));
                        client.sentProperty().sort((email1, email2) ->
                                email2.getDateTime().compareTo(email1.getDateTime()));
                    });

                    //ora esaminiamo tutte le nostre e-mail per verificare se ne abbiamo alcune che non sono più nel server
                    Platform.runLater(() -> {
                        client.inboxProperty().removeIf(inboxEmail -> !containsID(emailsFromServer, inboxEmail, Email.EmailState.RECEIVED));
                        client.sentProperty().removeIf(sentEmail -> !containsID(emailsFromServer, sentEmail, Email.EmailState.SENT));
                    });

                    if (newMails.get() > 0) {
                        Platform.runLater(() -> {
                            Alert a = new Alert(Alert.AlertType.INFORMATION, "Hai " + newMails + " nuova Emails!");
                            a.show();
                        });
                    }
                } else //Azzione non completata
                {
                    System.out.println("il caricamento ha causato un problema");
                }
            }
        }).start();
    }

    /**
     * Questo metodo verifica se un determinato ID di email (inboxEmail.getID()) è
     * già presente nella lista emailsFromServer dello stato specificato (emailState).
     * Restituisce true se l'ID è già presente, altrimenti false.
     */
    private boolean containsID(ArrayList<Email> emailsFromServer, Email inboxEmail, Email.EmailState emailState) {
        for (Email email : emailsFromServer) {
            if (email.getState() == emailState && inboxEmail.getID() == email.getID()) return true;
        }
        return false;
    }

    /**
     * Questo metodo crea una nuova connessione socket al server
     * utilizzando l'indirizzo IP localhost e la porta 6969.
     * Inoltre, inizializza gli stream di input e output per la comunicazione con il server.
     */
    protected void getNewSocket() throws IOException {
        socket = new Socket(InetAddress.getLocalHost(), 6969);
        objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
        objectInputStream = new ObjectInputStream(socket.getInputStream());
    }

    /**
     * Questo metodo viene chiamato quando la connessione al server è
     * riuscita con successo. Aggiorna la proprietà statusProperty del client a
     * "Connected" e imposta la visibilità del pulsante restartConnButton su false.
     */
    protected void setSocketSuccess() {
        Platform.runLater(() -> {
            client.statusProperty().setValue("Connected");
            restartConnButton.setVisible(false);
        });
    }

    /**
     * Questo metodo viene chiamato quando la connessione al server "fallisce".
     * Aggiorna la proprietà statusProperty del client a "Trying to connect to Server..."
     * e imposta la visibilità del pulsante restartConnButton su true
     */
    protected void setSocketFailure() {
        Platform.runLater(() -> {
            client.statusProperty().setValue("Tentativo di connessione al server...");
            restartConnButton.setVisible(true);
        });
    }

    /**
     * Questo metodo chiude la connessione con il server chiudendo il socket e
     * gli stream di input/output.
     */
    protected void closeConnectionToServer() {
        if (socket != null && objectInputStream != null && objectOutputStream != null) {
            try {
                socket.close();
                objectOutputStream.close();
                objectInputStream.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Questo metodo avvia un task periodico (PeriodicEmailDownloader)
     * che esegue il metodo loadAllFromServer() ogni 5 secondi.
     */
    public void startPeriodicEmailDownloader() {
        if (scheduledExEmailDownloader != null) return;
        scheduledExEmailDownloader = Executors.newScheduledThreadPool(1);
        scheduledExEmailDownloader.scheduleAtFixedRate(new PeriodicEmailDownloader(), 0, 5, TimeUnit.SECONDS);
    }

    //Questo metodo arresta il task periodico.
    public void shutdownPeriodicEmailDownloader() {
        if (scheduledExEmailDownloader != null)
            scheduledExEmailDownloader.shutdown();
    }

    /**
     * Questo metodo viene chiamato quando l'utente seleziona l'opzione
     * per visualizzare la casella di posta in arrivo (showInbox)
     */
    public void showInbox(ActionEvent actionEvent) {
        client.selectedEmail = new Email(client.getID() + 1);
        if (client.inboxProperty().size() > 0) client.selectedEmail = client.inboxProperty().get(0);
        folderListView.itemsProperty().bind(client.inboxProperty());
    }

    /**
     * Questo metodo viene chiamato quando l'utente seleziona l'opzione
     * per visualizzare le email inviate (showSent)
     */
    public void showSent(ActionEvent actionEvent) {
        client.selectedEmail = new Email(client.getID() + 1);
        if (client.sentProperty().size() > 0) client.selectedEmail = client.sentProperty().get(0);
        folderListView.itemsProperty().bind(client.sentProperty());
        bindMailToView(client.selectedEmail);
    }

    /**
     * Questa è una classe interna che implementa l'interfaccia Runnable
     * e viene utilizzata per eseguire il metodo loadAllFromServer() periodicamente.
     */
    class PeriodicEmailDownloader implements Runnable {
        public PeriodicEmailDownloader() {
        }

        @Override
        public void run() {
            loadAllFromServer();
        }
    }

    /**
     * Questo metodo invia un'azione (Action) al server attraverso l'oggetto
     * objectOutputStream
     */
    public void sendActionToServer(Action action) {
        if (objectOutputStream == null) return;
        try {
            objectOutputStream.writeObject(action);
            objectOutputStream.flush();
        } catch (Exception e) {
            System.out.println("Socket chiuso");
        }
    }

    /**
     * Questo metodo invia un'email serializzata (SerializableEmail)
     * al server attraverso l'oggetto objectOutputStream
     */
    public void sendEmailToServer(SerializableEmail serializableEmail) {
        if (objectOutputStream == null) return;
        try {
            objectOutputStream.writeObject(serializableEmail);
            objectOutputStream.flush();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Questo metodo attende la risposta dal server e restituisce
     * il  ServerResponse.
     */
    public ServerController.ServerResponse waitForResponse() throws IOException, ClassNotFoundException {
        ServerController.ServerResponse response;
        response = (ServerController.ServerResponse) objectInputStream.readObject();
        return response;
    }


}