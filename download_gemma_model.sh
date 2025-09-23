#!/bin/bash

# Script to download the actual Gemma 270M ONNX model
# This replaces the Git LFS placeholder with the real model

echo "🔄 Downloading Gemma 270M ONNX Model..."
echo "This may take a while depending on your internet connection."
echo ""

# Create models directory if it doesn't exist
mkdir -p models

# Backup the current placeholder file
if [ -f "models/gemma-270m.onnx" ]; then
    echo "📦 Backing up placeholder file..."
    mv "models/gemma-270m.onnx" "models/gemma-270m.onnx.backup"
fi

# Download the real Gemma 270M ONNX model from Hugging Face
# Note: Replace this URL with the actual Gemma ONNX model URL when available
# For now, we'll create instructions for manual download

echo "📋 MANUAL DOWNLOAD REQUIRED:"
echo ""
echo "1. Go to Hugging Face: https://huggingface.co/google/gemma-2b"
echo "2. Or search for 'Gemma 270M ONNX' model on Hugging Face"
echo "3. Download the ONNX format model file"
echo "4. Save it as: $(pwd)/models/gemma-270m.onnx"
echo ""
echo "Alternative: If you have the model file elsewhere, copy it to:"
echo "$(pwd)/models/gemma-270m.onnx"
echo ""
echo "Expected file size: Several hundred MB to few GB"
echo "Current placeholder size: $(ls -lh models/gemma-270m.onnx.backup 2>/dev/null | awk '{print $5}' || echo 'unknown')"
echo ""

# Check if user has already downloaded a real model
read -p "❓ Have you already downloaded the Gemma ONNX model file? (y/N): " has_model

if [[ $has_model =~ ^[Yy]$ ]]; then
    read -p "📁 Enter the path to your Gemma ONNX model file: " model_path
    
    if [ -f "$model_path" ]; then
        echo "📋 Copying model file..."
        cp "$model_path" "models/gemma-270m.onnx"
        
        # Check if the copy was successful and the file is substantial
        if [ -f "models/gemma-270m.onnx" ] && [ $(stat -f%z "models/gemma-270m.onnx" 2>/dev/null || stat -c%s "models/gemma-270m.onnx" 2>/dev/null) -gt 1000000 ]; then
            echo "✅ Model file copied successfully!"
            echo "📊 File size: $(ls -lh models/gemma-270m.onnx | awk '{print $5}')"
            echo ""
            echo "🔧 Next steps:"
            echo "1. Run: gradle clean build"
            echo "2. Run: gradle run"
            echo "3. Test the AI responses in the application"
        else
            echo "❌ Error: Model file seems too small or copy failed"
            echo "Please ensure you have the correct ONNX model file"
        fi
    else
        echo "❌ Error: File not found at $model_path"
    fi
else
    echo ""
    echo "📝 To continue with integration:"
    echo "1. Download the real Gemma ONNX model"
    echo "2. Place it at: $(pwd)/models/gemma-270m.onnx"
    echo "3. Run this script again"
    echo "4. Or run: gradle clean build && gradle run"
fi

echo ""
echo "🔧 Development Note:"
echo "The application will automatically detect when a real model is available"
echo "and switch from placeholder responses to actual AI-generated responses."
