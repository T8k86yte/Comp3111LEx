module project.library {
    requires javafx.controls;
    requires java.desktop;
    requires java.sql;

    exports project;
    exports project.ui;
    exports project.task1.ui;
    exports project.task2.ui.javafx;
    exports project.task3.ui;

    opens project.task1.model to javafx.base;
    opens project.task2.model to javafx.base;
}
