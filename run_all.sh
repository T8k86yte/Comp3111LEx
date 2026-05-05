#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo ""
echo "=========================================="
echo "  Library Management System - Unified"
echo "=========================================="
echo ""

cd /Users/yipyufung/Desktop/Programming/Java/COMP3111_Project_file/Comp3111_project_new

# Kill any existing Java processes to prevent file locks
echo -e "${YELLOW}🔍 Checking for existing Java processes...${NC}"
pkill -9 java 2>/dev/null && echo -e "${GREEN}✅ Killed existing Java processes${NC}" || echo -e "${BLUE}ℹ️ No existing Java processes found${NC}"
pkill -9 javac 2>/dev/null
pkill -9 mvn 2>/dev/null

# Wait for processes to fully terminate
sleep 2

# Remove target directory if it exists and is locked
if [ -d "target" ]; then
    echo -e "${YELLOW}📁 Removing existing target directory...${NC}"
    rm -rf target/ 2>/dev/null
    if [ -d "target" ]; then
        echo -e "${YELLOW}⚠️ Target directory locked, trying force removal...${NC}"
        # Try to remove with more force
        find target -type f -exec chmod 777 {} \; 2>/dev/null
        rm -rf target/ 2>/dev/null
    fi
    echo -e "${GREEN}✅ Target directory removed${NC}"
fi

echo ""
echo -e "${BLUE}📦 Compiling project...${NC}"
mvn clean compile

if [ $? -eq 0 ]; then
    echo ""
    echo -e "${GREEN}✅ Compilation successful!${NC}"
    echo ""
    echo -e "${BLUE}🚀 Starting Unified Library System...${NC}"
    echo "=========================================="
    echo ""
    
    # Clear any leftover session data
    rm -f data/session.dat 2>/dev/null
    rm -f data/session_recovery.txt 2>/dev/null
    
    mvn exec:java -Dexec.mainClass="project.LibrarySys"
else
    echo ""
    echo -e "${RED}❌ Compilation failed!${NC}"
    echo ""
    echo -e "${YELLOW}Try the following:${NC}"
    echo "  1. Close all other Java applications"
    echo "  2. Run: 'sudo rm -rf target/'"
    echo "  3. Restart your terminal"
    exit 1
fi
