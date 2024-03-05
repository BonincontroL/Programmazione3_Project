module com.prog3.prog3_project {
    requires javafx.controls;
    requires javafx.fxml;

    requires javafx.graphics;
    requires json.simple;
    requires javafx.base;


    exports com.prog3.prog3_project.client.view;
    opens com.prog3.prog3_project.client.view to javafx.fxml;

    exports com.prog3.prog3_project.client.controller;
    opens com.prog3.prog3_project.client.controller to javafx.fxml;

    exports com.prog3.prog3_project.client.model;
    opens com.prog3.prog3_project.client.model to javafx.fxml;


    exports com.prog3.prog3_project.server.controller;
    opens com.prog3.prog3_project.server.controller to javafx.fxml;
    exports com.prog3.prog3_project.server.view;
    opens com.prog3.prog3_project.server.view to javafx.fxml;






}