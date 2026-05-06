#!/bin/bash

echo "=========================================="
echo "  Running Author Portal (Task 2) with Maven"
echo "=========================================="

cd /Users/yipyufung/Desktop/Programming/Java/COMP3111_Project_file/Comp3111_project_new

mvn clean compile
mvn exec:java -Dexec.mainClass="project.task2.AuthorPortalFX"
