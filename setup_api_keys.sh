#!/bin/bash

echo "=========================================="
echo "  LLM API Key Setup for Book Summaries"
echo "=========================================="
echo ""
echo "To use real AI for generating book summaries, set one of these API keys:"
echo ""
echo "Option 1: OpenAI (recommended)"
echo "  export OPENAI_API_KEY='your-openai-api-key-here'"
echo ""
echo "Option 2: Google Gemini"
echo "  export GEMINI_API_KEY='your-gemini-api-key-here'"
echo ""
echo "To get API keys:"
echo "  - OpenAI: https://platform.openai.com/api-keys"
echo "  - Google Gemini: https://makersuite.google.com/app/apikey"
echo ""
echo "Example:"
echo "  export OPENAI_API_KEY='sk-...'"
echo "  then run: ./run.sh"
echo ""

# Check current status
if [ -n "$OPENAI_API_KEY" ]; then
    echo "✅ OPENAI_API_KEY is set"
elif [ -n "$GEMINI_API_KEY" ]; then
    echo "✅ GEMINI_API_KEY is set"
else
    echo "⚠️ No API keys set. Will use mock mode (simulated AI)"
fi
