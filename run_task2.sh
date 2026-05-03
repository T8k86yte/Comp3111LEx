#!/bin/bash

echo "=========================================="
echo "  Running Author Portal (Task 2)"
echo "=========================================="

cd /Users/yipyufung/Desktop/Programming/Java/COMP3111_Project_file/Comp3111_project_new

# Set JavaFX path
export PATH_TO_FX=/Library/Java/JavaFX/javafx-sdk-21.0.10/lib

# Clean and compile with Maven
mvn clean compile

if [ $? -eq 0 ]; then
    echo ""
    echo "Starting Author Portal..."
    echo "=========================================="
    echo ""
    
    # Run with classpath including lib directory for PDFBox
    java -cp "target/classes:lib/*:$PATH_TO_FX/*" project.task2.AuthorPortalFX
else
    echo "Compilation failed!"
    exit 1
fi
