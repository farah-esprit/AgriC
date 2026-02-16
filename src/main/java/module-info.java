module com.eventmanagement {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    
    opens com.eventmanagement to javafx.fxml;
    opens com.eventmanagement.controllers to javafx.fxml;
    opens com.eventmanagement.models to javafx.base;
    
    exports com.eventmanagement;
    exports com.eventmanagement.controllers;
    exports com.eventmanagement.models;
}
