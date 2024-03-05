package com.prog3.prog3_project.client.view;

import com.prog3.prog3_project.client.controller.ClientController;
import com.prog3.prog3_project.client.controller.NewMsgController;
import com.prog3.prog3_project.client.model.Client;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class ClientView extends Application {

    private static String clientEmail; // Campo statico per memorizzare l'email del client
    private static final List<String> validEmails = Arrays.asList(
            "Mario.rossi@gmail.com", "Luca.verdi@gmail.com", "Simone.neri@libero.it"
    ); // Lista di indirizzi email validi  >> !!!SOLO per scopo di prova... @ToDo collegamento e controllo con DB

    public static void main(String[] args) {
        if (args.length > 0) {
            clientEmail = args[0]; // Legge l'indirizzo email dalla riga di comando
        }
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        if (clientEmail != null && validEmails.contains(clientEmail)) {
            // Crea il client con l'indirizzo email specificato
            Client client = new Client(clientEmail);
            createClientWindow(client, "Mail Sender - " + clientEmail, 750, 510);
        } else {
            System.out.println("Indirizzo email non riconosciuto o non fornito.");
            if (!validEmails.isEmpty()) {
                System.out.println("Indirizzi email disponibili:");
                for (String email : validEmails) {
                    System.out.println("- " + email);
                }
            } else {
                System.out.println("Nessun indirizzo email disponibile.");
            }
            System.out.println("Passare l'indirizzo email come parametro tramite args!");
            System.exit(0);
        }
    }


    private void createClientWindow(Client client, String title, double width, double height) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("ClientView.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), width, height);
        ClientController controller = fxmlLoader.getController();

        // Configura il controller principale con il client
        // e imposta il binding per lo stato e le proprietà del client
        controller.setClient(client);
        controller.setStatusBiding();
        controller.bindClientProperties();

        FXMLLoader fxmlLoader1 = new FXMLLoader(getClass().getResource("newMsgView.fxml"));
        Parent v = fxmlLoader1.load();
        NewMsgController controller1 = fxmlLoader1.getController();
        controller1.setClient(client);
        controller1.setClientController(controller);
        Scene scene1 = new Scene(v, 600, 400);
        Stage newStage = new Stage();

        newStage.setOnShown((event) -> controller1.bindMsg());
        newStage.setScene(scene1);

        newStage.setTitle("New Email");
        controller.setStage(newStage);

        Stage clientStage = new Stage();
        clientStage.setOnShown((event) -> controller.startPeriodicEmailDownloader());
        clientStage.setOnCloseRequest((windowEvent) -> controller.shutdownPeriodicEmailDownloader());
        clientStage.setTitle(title);
        clientStage.setScene(scene);

        clientStage.show();
    }
}