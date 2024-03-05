package com.prog3.prog3_project.server.controller;

import com.prog3.prog3_project.client.model.Client;
import com.prog3.prog3_project.client.model.Email;
import com.prog3.prog3_project.client.model.SerializableEmail;
import com.prog3.prog3_project.server.model.Server;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import com.prog3.prog3_project.server.model.Action;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.SocketException;
import java.util.ArrayList;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class ServerController {

    /**
     * Questa enum rappresenta un elenco di possibili risultati dopo che è stata effettuata una richiesta al server
     * Viene utilizzata per comunicare al client l'esito dell'operazione
     * */
    public enum ServerResponse {
        ACTION_COMPLETED,//Ok tutto bene :)
        RECEIVER_NOT_FOUND,// email client non trovato , non è registrato
        UNKNOWN_ERROR,//ERORR, no bene :(
        EMAIL_NOT_FOUND,//ERORR email
        CLIENT_NOT_FOUND,//"Error" new client
    }

    private Server server; //Model
    private ServerSocket serverSocket;
    private ExecutorService executorService;
    private ScheduledExecutorService scheduledExecutorService;

    @FXML
    public Button startServerButton;
    @FXML
    public Button stopServerButton;
    @FXML
    public TextArea logTextArea;


    public void initialize() {
        if (this.server != null)
            throw new IllegalStateException("il server può essere inizializzato solo una volta");
        this.server = new Server();
        startServerButton.setDisable(true);
        logTextArea.textProperty().bind(server.logProperty());
        startServer();
    }

    /**
     * Questo metodo viene richiamato dall'evento clic sul pulsante
     * per consentire all'utente di avviare il Server tramite:
     * Impostando lo stato di esecuzione su true,
     * Disabilita il pulsante di avvio perché è già stato cliccato
     * imposta su Visibile il pulsante di stop e infine chiama il metodo
     * startServer
     */
    @FXML
    private void onStartServerClicked() {
        server.setRunning(true);
        startServerButton.setDisable(true);
        stopServerButton.setDisable(false);
        startServer();
    }

    @FXML
    private void onStopServerClicked() {
        server.setRunning(false);
        stopServerButton.setDisable(true);
        startServerButton.setDisable(false);
        stopServer();
    }

    /**
     * stopServer è il metodo che spegne
     * ogni pool creato da startServer e
     * salva le informazioni di tutti i clienti nel file JSON
     */
    public void stopServer() {
        server.setRunning(false);
        executorService.shutdown();
        scheduledExecutorService.shutdown();
        server.saveClientsToJSON();
        try {
            serverSocket.close();
        } catch (IOException ioException) {
            System.out.println("Chiusura del server!");
        }
        server.updateLog(">> Chiusura del server! " + '\n');
    }

    private Client findClientByAddress(String address) {
        Client sender = null;
        ArrayList<Client> clients = server.getClients();
        int i = 0;
        while ((sender == null) && i < clients.size()) {
            if (clients.get(i).getAddress().equals(address)) sender = clients.get(i);
            i++;
        }
        return sender;
    }


    /**
     * startServer è un metodo
     * che crea un serverSocket,
     * carica le informazioni di tutti i clienti dal JSON,
     * crea un pool di thread utilizzati per catturare ed eseguire la connessione della richiesta del client,
     * crea un pool pianificato di thread per salvare le informazioni dei clienti ogni 30 secondi, avviato con un ritardo di 30 secondi.
     * Infine, per consentire all'utente di utilizzare la vista creiamo un Single Thread che gestisce la fase di Connessione
     */
    private void startServer() {
        try {
            serverSocket = new ServerSocket(6969);
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }
        server.updateLog(">> START SERVER!... " + '\n');
        server.readFromJSONFile();
        executorService = Executors.newFixedThreadPool(10);
        scheduledExecutorService = Executors.newScheduledThreadPool(1);
        scheduledExecutorService.scheduleAtFixedRate(new SaveAllTask(), 30, 30, TimeUnit.SECONDS);

        new Thread(() -> {
            while (server.isRunning()) {
                try {
                    Socket incomingRequestSocket = serverSocket.accept();
                    ServerTask st = new ServerTask(incomingRequestSocket);
                    executorService.execute(st);
                } catch (SocketException socketException) {
                    System.out.println("Chiusura del Socket");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }).start();
    }


    public void clearServerLog(ActionEvent actionEvent) {
        server.logProperty().setValue("");
    }

    /**
     * ServerTask è l'attività che verrà eseguita,
     * questa attività contiene un'azione inviata dal Cliente utilizzando
     * ObjectOutputStream, Server in base alla richiesta inviata
     * eseguirà un'operazione e invierà una risposta al Cliente
     */
    class ServerTask implements Runnable {
        Socket socketS;
        ObjectOutputStream objectOutputStream;
        ObjectInputStream objectInputStream;
        boolean allInitialized = false;

        public ServerTask(Socket socketS) {
            this.socketS = socketS;
            try {
                objectOutputStream = new ObjectOutputStream(socketS.getOutputStream());
                objectInputStream = new ObjectInputStream(socketS.getInputStream());
                allInitialized = true;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        @Override
        public void run() {
            if (!allInitialized) return;
            try {
                Action actionRequest = (Action) objectInputStream.readObject();
                ServerResponse response;
                if (actionRequest.getOperation() == Action.Operation.SEND_EMAIL) {
                    response = addEmailToReceiversInbox(actionRequest, objectInputStream);
                    sendResponse(response);
                    server.add(actionRequest);
                    server.saveClientsToJSON();
                } else if (actionRequest.getOperation() == Action.Operation.DELETE_EMAIL) {
                    response = deleteEmail(actionRequest, objectInputStream);
                    sendResponse(response);
                    server.add(actionRequest);
                    server.saveClientsToJSON();
                } else if (actionRequest.getOperation() == Action.Operation.READ_EMAIL) {
                    response = setEmailAsRead(actionRequest, objectInputStream);
                    sendResponse(response);
                    server.add(actionRequest);
                    server.saveClientsToJSON();
                } else if (actionRequest.getOperation() == Action.Operation.GET_ALL_EMAILS) {
                    //L'unico metodo nullo perché invia la risposta prima di inviare tutte le email
                    sendAllEmails(actionRequest);
                    server.add(actionRequest);
                }
            } catch (Exception ex) {
                synchronized (logTextArea) {
                    server.updateLog(" Errore nell'elaborazione della richiesta " + '\n');
                }
                ex.printStackTrace();
            } finally {
                try {
                    objectOutputStream.close();
                    objectInputStream.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        /**
         * Metodo utilizzato per l'invio
         * una risposta al Cliente utilizzando
         * l'oggetto socketOutputStream
         *
         * @param response riposta che verrà inviata al Cliente
         */
        private void sendResponse(ServerResponse response) {
            try {
                objectOutputStream.writeObject(response);
                objectOutputStream.flush();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        /**
         * addEmailToReceiversInbox viene utilizzato per inviare un'e-mail,
         * per prima cosa leggiamo SerializableEmail che è l'e-mail da inviare al/ai destinatario/i, cerchiamo nel nostro elenco clienti s
         * e esiste utilizzando findClientByAddress, quindi aggiungiamo alla proprietà della posta in arrivo del cliente locale (destinatario) inboxEmail e
         * aggiungiamo alla proprietà locale Il client (mittente) ha inviato la proprietà sentEmail infine inviamo la risposta:
         * se tutto è stato eseguito correttamente ACTION_COMPLETED
         *
         * @param actionRequest l'azione inviata dal client
         * @param inStream      utilizzato per leggere la SerializableEmail inviata dal client
         * @returnServerResponse
         */
        ServerResponse addEmailToReceiversInbox(Action actionRequest, ObjectInputStream inStream) {
            try {
                SerializableEmail serializableEmail = (SerializableEmail) inStream.readObject();
                Email sentEmail = new Email(serializableEmail);
                sentEmail.setReceiver(sentEmail.getReceiver().replaceAll(" ", ""));

                //client su cui operare
                Client sender = findClientByAddress(actionRequest.getSender());

                sentEmail.setState(Email.EmailState.SENT);
                String[] receiversTmp = actionRequest.getReceiver().split(",");
                ArrayList<Client> receivers = new ArrayList<>();

                //controlla se ci sono ricevitori null
                for (int i = 0; i < receiversTmp.length; i++) {
                    Client client = findClientByAddress(receiversTmp[i].strip());
                    if (receiversTmp[i] == null || client == null) {
                        actionRequest.setSuccessful(false);
                        return ServerResponse.RECEIVER_NOT_FOUND;
                    }
                    receivers.add(client);
                    receiversTmp[i] = receiversTmp[i].strip();
                }

                for (int j = 0; j < receiversTmp.length; j++) {
                    Client receiver = receivers.get(j);

                    Email inboxEmail = sentEmail.clone();
                    inboxEmail.setState(Email.EmailState.RECEIVED);
                    inboxEmail.setRead(false);

                    inboxEmail.setID(receiver.getID() + 1);
                    receiver.inboxProperty().add(inboxEmail);
                }
                sentEmail.setID(sender.getID() + 1);
                sender.sentProperty().add(sentEmail);
                actionRequest.setSuccessful(true);
                return ServerResponse.ACTION_COMPLETED;
            } catch (Exception ex) {
                ex.printStackTrace();
                actionRequest.setSuccessful(false);
                return ServerResponse.UNKNOWN_ERROR;
            }
        }

        /**
         * deleteEmail viene utilizzato per eliminare un'e-mail,
         * prima leggiamo la SerializableEmail che è l'Email da eliminare,
         * cerchiamo nel nostro elenco clienti se esiste utilizzando findClientByAddress,
         * dobbiamo sapere dove si trova l'e-mail, utilizziamo un metodo Client whereIs
         * che restituiscono la SimpleListProperty che contiene l'e-mail
         * e poi lo rimuoviamo dall'elenco
         * @param actionRequest l'azione inviata dal client
         * @param inStream      utilizzato per leggere la SerializableEmail inviata dal client
         * @return ServerResponse
         */
        ServerResponse deleteEmail(Action actionRequest, ObjectInputStream inStream) {
            try {
                SerializableEmail serializableEmail = (SerializableEmail) inStream.readObject();
                Email emailToBeDeleted = new Email(serializableEmail);

                //Ricerca il client che ha richiesto l'eliminazione
                Client sender = findClientByAddress(actionRequest.getSender());
                if (sender == null) {
                    actionRequest.setSuccessful(false);
                    return ServerResponse.CLIENT_NOT_FOUND;
                }
                SimpleListProperty<Email> list = sender.whereIs(emailToBeDeleted);
                if (list == null) {
                    actionRequest.setSuccessful(false);
                    return ServerResponse.EMAIL_NOT_FOUND;
                } else {
                    list.remove(emailToBeDeleted);
                    actionRequest.setSuccessful(true);
                    return ServerResponse.ACTION_COMPLETED;
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                actionRequest.setSuccessful(false);
                return ServerResponse.UNKNOWN_ERROR;
            }

        }

        /**
         * sendAllEmails è il metodo che consente a un Client
         * per scaricare ogni email contenuta nelle diverse sezioni
         * se il Client è nuovo, lo salviamo su file JSON ma poiché è nuovo,
         * non ha email, quindi inviamo CLIENT_NOT_FOUND come risposta.
         * Altrimenti inviamo al Client ogni singola Email (trovabile localmente)
         * utilizzando objectOutputStream
         *
         * @param actionRequest l'azione inviata dal client
         */
        void sendAllEmails(Action actionRequest) {
            try {
                Client requestClient = findClientByAddress(actionRequest.getSender());

                if (requestClient == null) {
                    server.addClient(new Client(actionRequest.getSender()));
                    server.saveClientsToJSON();
                    sendResponse(ServerResponse.CLIENT_NOT_FOUND);
                    actionRequest.setSuccessful(false);
                    return;
                }
                // tutte le email di un determinato cliente
                SimpleListProperty<Email> allEmails = new SimpleListProperty<>(FXCollections.observableArrayList());
                allEmails.addAll(requestClient.inboxProperty());
                allEmails.addAll(requestClient.sentProperty());

                sendResponse(ServerResponse.ACTION_COMPLETED);
                actionRequest.setSuccessful(true);

                //Inviati tutti attraverso il socket
                for (Email email : allEmails) {
                    SerializableEmail serializableEmail = new SerializableEmail(email);
                    objectOutputStream.writeObject(serializableEmail);
                    objectOutputStream.flush();
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * setEmailAsRead è il metodo che gestisce lo stato di lettura di un'e-mail,
     * prima leggiamo SerializableEmail da objectInputStream,
     * cerchiamo se il Cliente esiste,
     * Se è vero, cerchiamo solo nella sezione inbox perché è un'e-mail non letta
     * può essere solo lì, in caso contrario inviamo EMAIL_NOT_FOUND come risposta
     * altrimenti impostiamo lo stato dell'email su true e salviamo le informazioni nel file JSON,
     * e infine invia ACTION_COMPLETED come risposta.
     *
     * @param actionRequest     l'azione inviata dal client
     * @param objectInputStream utilizzato per leggere la SerializableEmail inviata dal client
     * @return ServerResponse
     */
    private ServerResponse setEmailAsRead(Action actionRequest, ObjectInputStream objectInputStream) {
        try {
            SerializableEmail serializableEmail = (SerializableEmail) objectInputStream.readObject();
            long id = serializableEmail.getID();

            Client sender = findClientByAddress(actionRequest.getSender());
            if (sender == null) {
                actionRequest.setSuccessful(false);
                return ServerResponse.CLIENT_NOT_FOUND;
            }

            //Controlliamo solo la posta in arrivo poiché un'e-mail non letta può essere solo lì
            Email email = sender.findEmailById(sender.inboxProperty(), id);
            if (email == null) {
                actionRequest.setSuccessful(false);
                return ServerResponse.EMAIL_NOT_FOUND;
            }

            email.setRead(true);
            server.saveClientsToJSON();
            actionRequest.setSuccessful(true);
            return ServerResponse.ACTION_COMPLETED;
        } catch (Exception ex) {
            ex.printStackTrace();
            actionRequest.setSuccessful(false);
            return ServerResponse.UNKNOWN_ERROR;
        }
    }

    /**
     * Usiamo questa classe eseguibile
     * per salvare le informazioni dei client utilizzando
     * il suo metodo run() che chiama
     * funzione saveClientsToJSON
     */
    class SaveAllTask implements Runnable {
        public SaveAllTask() {
        }

        @Override
        public void run() {
            server.saveClientsToJSON();
        }
    }


}
