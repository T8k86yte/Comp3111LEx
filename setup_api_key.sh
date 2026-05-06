#!/bin/bash

# DeepSeek API Key for Task 2 - LLM Summary Generation
DEEPSEEK_API_KEY="sk-e5ec887c181f4ec8acc63bf130e28ee5"

echo ""
echo "=========================================="
echo "  Task 2 - DeepSeek API Key Setup"
echo "  For LLM Book Summary Generation"
echo "=========================================="
echo ""

# Set for current session
export DEEPSEEK_API_KEY="$DEEPSEEK_API_KEY"
echo "✅ API key set for current terminal session"

# Ask to save permanently
echo ""
read -p "Save to ~/.zshrc for future sessions? (y/n): " save_permanent

if [ "$save_permanent" = "y" ] || [ "$save_permanent" = "Y" ]; then
    # Remove existing entry if present
    sed -i '' '/export DEEPSEEK_API_KEY=/d' ~/.zshrc 2>/dev/null
    # Add new entry
    echo "export DEEPSEEK_API_KEY=\"$DEEPSEEK_API_KEY\"" >> ~/.zshrc
    echo "✅ API key saved to ~/.zshrc"
fi

echo ""
echo "=========================================="
echo "Setup complete!"
echo ""
echo "Current API key: ${DEEPSEEK_API_KEY:0:10}..."
echo ""
echo "To run the Author Portal with real AI:"
echo "  ./run_all.sh"
echo "=========================================="
