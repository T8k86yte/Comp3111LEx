#!/bin/bash

echo "=========================================="
echo "  Library Management System - Unified"
echo "=========================================="
echo ""

cd /Users/yipyufung/Desktop/Programming/Java/COMP3111_Project_file/Comp3111_project_new

echo "📦 Compiling project..."
mvn clean compile

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Compilation successful!"
    echo ""
    echo "🚀 Starting Unified Library System..."
    echo "=========================================="
    echo ""
    mvn exec:java -Dexec.mainClass="project.LibrarySys"
else
    echo ""
    echo "❌ Compilation failed!"
    exit 1
fi
