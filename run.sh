#!/bin/bash
export PATH_TO_FX=/Library/Java/JavaFX/javafx-sdk-21.0.10/lib
rm -rf target/classes
mkdir -p target/classes
javac --module-path $PATH_TO_FX --add-modules javafx.controls -d target/classes src/main/java/project/task2/model/*.java
javac --module-path $PATH_TO_FX --add-modules javafx.controls -cp target/classes -d target/classes src/main/java/project/task2/repo/*.java
javac --module-path $PATH_TO_FX --add-modules javafx.controls -cp target/classes -d target/classes src/main/java/project/task2/utils/*.java
javac --module-path $PATH_TO_FX --add-modules javafx.controls -cp target/classes -d target/classes src/main/java/project/task2/service/*.java
javac --module-path $PATH_TO_FX --add-modules javafx.controls,javafx.fxml -cp target/classes -d target/classes src/main/java/project/task2/ui/javafx/*.java
javac --module-path $PATH_TO_FX --add-modules javafx.controls,javafx.fxml -cp target/classes -d target/classes src/main/java/project/task2/AuthorPortalFX.java
cp -r src/main/resources/project target/classes/
mkdir -p data
java --module-path $PATH_TO_FX --add-modules javafx.controls,javafx.fxml -cp target/classes project.task2.AuthorPortalFX
