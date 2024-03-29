module store.bubbletill.pos {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.ikonli.javafx;
    requires eu.hansolo.tilesfx;
    requires com.google.gson;
    requires org.apache.httpcomponents.httpclient;
    requires org.apache.httpcomponents.httpcore;
    requires commons;
    requires java.sql;

    opens store.bubbletill.pos to javafx.fxml;
    opens store.bubbletill.pos.controllers to javafx.fxml;
    exports store.bubbletill.pos;
    exports store.bubbletill.pos.controllers;
    exports store.bubbletill.pos.exceptions;
    exports store.bubbletill.pos.controllers.admin;
    opens store.bubbletill.pos.controllers.admin to javafx.fxml;
}