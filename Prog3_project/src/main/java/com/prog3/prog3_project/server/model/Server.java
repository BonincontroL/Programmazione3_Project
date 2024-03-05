package com.prog3.prog3_project.server.model;

import com.prog3.prog3_project.client.model.Client;
import com.prog3.prog3_project.client.model.Email;
import javafx.application.Platform;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class Server {

    ArrayList<Action> actions; //Una lista di oggetti Action che rappresentano le azioni eseguite dal server
    ArrayList<Client> clients;//Una lista di oggetti Client che rappresentano i client connessi al server
    SimpleStringProperty log; //Proprietà SimpleStringProperty  utilizzata per aggiornare il log del serve
    private boolean running = true;

    private final String[] sectionNames = {"inbox", "sent"};

    public ArrayList<Action> getActions() {
        return actions;
    }

    public ArrayList<Client> getClients() {
        return clients;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public SimpleStringProperty logProperty() {
        return log;
    }

    public Server() {
        clients = new ArrayList<>();
        actions = new ArrayList<>();
        log = new SimpleStringProperty("");
    }

    /**
     * addClient viene utilizzato per aggiungere Client a
     * l'ArrayList dei client
     *
     * @param c che verrà aggiunto in ArrayList
     */
    public void addClient(Client c) {
        if (c != null)
            clients.add(c);
    }

    /**
     * Questo metodo statico accetta una stringa
     * della sezione (posta in arrivo, inviati) e
     * restituisce l'EmailState della sezione specifica
     *
     * @param s stringa da confrontare
     * @return EmailState
     */
    public static Email.EmailState stringToEmailState(String s) {
        if (s.compareTo("inbox") == 0) {
            return Email.EmailState.RECEIVED;
        }
        if (s.compareTo("sent") == 0) {
            return Email.EmailState.SENT;
        } else return null;
    }

    /**
     * Questo metodo ci permette di aggiungere un'Azione
     * all'ArrayList dell'azione e chiamare
     * updateLog per mostrare l'azione in vista
     *
     * @param Request è l'azione da mostrare
     */
    public synchronized void add(Action Request) {
        actions.add(Request);
        updateLog(Request.toString() + '\n');
    }

    /**
     * updateLog è un metodo che abbiamo utilizzato per aggiornare la visualizzazione del registro del server
     *
     * @param s è una stringa che verrà mostrata nella vista del Server
     */
    public void updateLog(String s) {
        Platform.runLater(() -> log.setValue(log.getValue() + s));
    }

    public boolean isRunning() {
        return running;
    }

    /**
     * Utilizziamo readFromJSONClientsFile per
     * leggere il JSON del client che è un file
     * che contiene tutte le informazioni del Client
     */
    public void readFromJSONFile() {
        clients = new ArrayList<>();
        System.out.println("Lettura Client dati...");
        JSONParser jsonParser = new JSONParser();//utilizzato per analizzare il file JSON.
        try (FileReader reader = new FileReader("src/main/resources/com/prog3/prog3_project/server/view/data/clients.json")) {
            Object obj = jsonParser.parse(reader);//Analizza il contenuto del file JSON e restituisce un oggetto generico Object rappresentante la struttura gerarchica del file.
            JSONArray clientsList = (JSONArray) obj;
            for (Object o : clientsList) {
                JSONObject jsonClient = (JSONObject) o;
                String clientString = jsonClient.toString();
                String[] x = clientString.split("\"");
                Client client = new Client(x[1]);
                clients.add(client);
                parseClientObject((JSONObject) o, client);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * parseClientObject è una funzione utilizzata da readFromJSonFile per prendere ogni
     * informazione di un singolo Client specifico e infine aggiungere la sua Email letta dal file JSON
     *
     * Questo metodo parseClientObject è progettato per estrarre e interpretare un
     * oggetto JSON rappresentante le informazioni di un client e le sue email,
     * quindi popolare un oggetto Client con queste informazioni.
     *
     * @param clientJson è il client passato come JSONObject utilizzato per prendere le sue informazioni
     * @param clientObject è il client passato come obj utilizzato per selezionare su JSONArray il client corretto
     */
    private void parseClientObject(JSONObject clientJson, Client clientObject) {

        // Ottiene Lista dati dal JSON corrispondente all'indirizzo del client
        JSONArray List = (JSONArray) clientJson.get(clientObject.getAddress());
        for (int i = 0; i < List.size(); i++) {
            // Ottiene un oggetto JSON rappresentante una sezione
            JSONObject sectionObj = (JSONObject) List.get(i);
            //Ottiene l'array di email dalla sezione corrispondente
            JSONArray emailList = (JSONArray) sectionObj.get(sectionNames[i]);

            for (Object o : emailList) {
                // Ottiene un oggetto JSON rappresentante una email
                JSONObject emailObj = (JSONObject) o;

                // Estrae le informazioni dalla email
                String sender = (String) emailObj.get("sender");
                String receiver = (String) emailObj.get("receiver");
                String subject = (String) emailObj.get("subject");
                String body = (String) emailObj.get("body");
                String dateTime = (String) emailObj.get("dateTime");
                long ID = (Long) emailObj.get("ID");

                boolean read = (boolean) emailObj.get("read");
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
                Email.EmailState emailState = stringToEmailState(sectionNames[i]);

                // Crea un oggetto Email con le informazioni estratte

                Email email = new Email(sender, receiver, subject, body, emailState, LocalDateTime.parse(dateTime, formatter), ID);

                email.setRead(read);
                // Aggiunge la email alla lista appropriata nel client
                switch (emailState) {
                    case RECEIVED -> clientObject.inboxProperty().add(email);
                    case SENT -> clientObject.sentProperty().add(email);
                }
            }
        }
    }

    /**
     * Questo metodo salva tutte le informazioni del Cliente
     * nel file JSON
     */
    public synchronized void saveClientsToJSON() {
        System.out.println("Salvataggio dati server...");
        JSONArray array = new JSONArray();

        // Itera attraverso tutti i client
        for (Client client : clients) {

            JSONObject clients = new JSONObject();// Rappresenta i dati del client
            JSONArray arrayOfsection = new JSONArray();// Contiene le sezioni (inbox, sent) del client

            JSONObject section; // Rappresenta una sezione (inbox o sent) del client
            JSONArray arrayOfEmail; // Contiene le email in una sezione
            JSONObject emailDetails;// Rappresenta i dettagli di una email

            // SimpleListProperty delle email in arrivo e inviate del client
            SimpleListProperty<Email>[] lists = new SimpleListProperty[]{client.inboxProperty(), client.sentProperty()};
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            int i = 0;

            for (SimpleListProperty<Email> list : lists) {

                arrayOfEmail = new JSONArray();

                for (Email email : list) {
                    emailDetails = new JSONObject();
                    emailDetails.put("sender", email.getSender());
                    emailDetails.put("receiver", email.getReceiver());
                    emailDetails.put("subject", email.getSubject());
                    emailDetails.put("body", email.getBody());
                    emailDetails.put("dateTime", email.getDateTime().format(formatter));
                    emailDetails.put("ID", email.getID());
                    emailDetails.put("read", email.isRead());
                    arrayOfEmail.add(emailDetails);
                }
                section = new JSONObject();
                section.put(sectionNames[i], arrayOfEmail);
                arrayOfsection.add(section);
                clients.put(client.getAddress(), arrayOfsection);
                i++;
            }

            array.add(clients);

            try {
                File file = new File("src/main/resources/com/prog3/prog3_project/server/view/data/clients.json");
                PrintWriter out = new PrintWriter(file);
                try {
                    out.write(array.toJSONString());
                    out.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    out.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


}
