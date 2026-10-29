module org.example.graphiceditor {
    requires javafx.controls;
    requires java.desktop;
    requires java.xml;

    exports org.example.graphiceditor;
    exports org.example.graphiceditor.model;
    exports org.example.graphiceditor.model.vector;
    exports org.example.graphiceditor.factory;
    exports org.example.graphiceditor.controller;
}
