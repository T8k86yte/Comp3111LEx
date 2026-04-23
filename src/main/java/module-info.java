module project.library {
    requires javafx.controls;
    requires javafx.swing;
    requires java.desktop;
    requires org.apache.pdfbox;
    requires org.apache.poi.poi;
    requires java.sql;
    requires langchain4j;
    requires langchain4j.open.ai;
    requires langchain4j.core;
    requires langchain4j.document.parser.apache.pdfbox;
    requires lombok;
    requires com.google.gson;
    requires okhttp3;

    exports project;
    exports project.ui;
    exports project.task1.ui;
    exports project.task2.ui.javafx;
    exports project.task3.ui;
    exports project.task3.model to lombok;

    opens project.task1.model to javafx.base;
    opens project.task2.model to javafx.base;
    opens project.task3.service to javafx.base;
}
