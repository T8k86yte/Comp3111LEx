#!/bin/bash

export PATH_TO_FX=/Library/Java/JavaFX/javafx-sdk-21.0.10/lib
PROJECT_DIR="$PWD"

rm -rf target/classes
mkdir -p target/classes

# JAR files
SQLITE_JAR="$PROJECT_DIR/lib/sqlite-jdbc-3.44.1.0.jar"
SLF4J_API="$PROJECT_DIR/lib/slf4j-api-2.0.9.jar"
SLF4J_SIMPLE="$PROJECT_DIR/lib/slf4j-simple-2.0.9.jar"

# Download if missing
download_if_missing() {
    if [ ! -f "$1" ]; then
        echo "📥 Downloading $(basename $1)..."
        curl -L -o "$1" "$2"
    fi
}

download_if_missing "$SQLITE_JAR" "https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.44.1.0/sqlite-jdbc-3.44.1.0.jar"
download_if_missing "$SLF4J_API" "https://repo1.maven.org/maven2/org/slf4j/slf4j-api/2.0.9/slf4j-api-2.0.9.jar"
download_if_missing "$SLF4J_SIMPLE" "https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/2.0.9/slf4j-simple-2.0.9.jar"

CLASSPATH="target/classes:$SQLITE_JAR:$SLF4J_API:$SLF4J_SIMPLE"

echo "📦 Compiling Task 2..."

# Compile Task 2 database package first
javac -cp "$CLASSPATH" -d target/classes src/main/java/project/task2/database/*.java

# Compile Task 1 models (required by Task 2)
javac -cp "$CLASSPATH" -d target/classes src/main/java/project/task1/model/*.java 2>/dev/null || true

# Compile Task 2 files
javac -cp "$CLASSPATH" -d target/classes \
    src/main/java/project/task2/model/*.java \
    src/main/java/project/task2/repo/*.java \
    src/main/java/project/task2/utils/*.java \
    src/main/java/project/task2/service/*.java

# Compile JavaFX UI
javac --module-path $PATH_TO_FX --add-modules javafx.controls,javafx.fxml \
     -cp "$CLASSPATH" -d target/classes \
     src/main/java/project/task2/ui/javafx/*.java \
     src/main/java/project/task2/AuthorPortalFX.java

# Copy resources
mkdir -p target/classes/project/task2/css
cp -r src/main/resources/project/task2/css/* target/classes/project/task2/css/ 2>/dev/null || true

mkdir -p data

echo "==================================="
echo "🚀 Running Author Portal (Task 2)"
echo "==================================="

java --module-path $PATH_TO_FX \
     --add-modules javafx.controls,javafx.fxml \
     -cp "target/classes:$SQLITE_JAR:$SLF4J_API:$SLF4J_SIMPLE" \
     project.task2.AuthorPortalFX
