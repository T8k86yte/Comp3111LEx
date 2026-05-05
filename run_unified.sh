#!/bin/bash

echo "=========================================="
echo "  Running Unified Library Application"
echo "  (All 3 Portals: Student/Staff, Author, Librarian)"
echo "=========================================="

cd /Users/yipyufung/Desktop/Programming/Java/COMP3111_Project_file/Comp3111_project_new

# Set JavaFX path
export PATH_TO_FX=/Library/Java/JavaFX/javafx-sdk-21.0.10/lib

# Clean and compile
mvn clean compile

if [ $? -eq 0 ]; then
    echo ""
    echo "Starting Unified Application..."
    echo "=========================================="
    echo ""
    
    # Run with classpath
    java -cp "target/classes:$PATH_TO_FX/*" project.LibrarySys
else
    echo "Compilation failed!"
    exit 1
fi
