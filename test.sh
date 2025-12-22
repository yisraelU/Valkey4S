#!/bin/bash

# Valkey4S Test Runner
# Checks Docker, starts it if needed, and runs tests

set -e

echo "🧪 Valkey4S Test Runner"
echo "======================"
echo ""

# Check if Docker is running
if ! docker ps > /dev/null 2>&1; then
    echo "❌ Docker is not running!"
    echo ""
    echo "To run integration tests, please:"
    echo "1. Start Docker Desktop (macOS/Windows)"
    echo "   OR"
    echo "2. Start Docker daemon (Linux): sudo systemctl start docker"
    echo ""
    echo "Running unit tests only (no Docker required)..."
    echo ""

    sbt glideCore/test

    echo ""
    echo "✅ Unit tests completed!"
    echo "⚠️  Integration tests skipped (Docker not available)"
    echo ""
    echo "To run integration tests:"
    echo "  ./test.sh --integration"
    exit 0
fi

echo "✅ Docker is running"
echo ""

# Check if user wants integration tests
if [[ "$1" == "--integration" ]] || [[ "$1" == "-i" ]]; then
    echo "Running all tests (unit + integration)..."
    echo ""
    sbt test
    echo ""
    echo "✅ All tests completed!"
elif [[ "$1" == "--unit" ]] || [[ "$1" == "-u" ]]; then
    echo "Running unit tests only..."
    echo ""
    sbt core/test
    echo ""
    echo "✅ Unit tests completed!"
elif [[ "$1" == "--watch" ]] || [[ "$1" == "-w" ]]; then
    echo "Running tests in watch mode..."
    echo ""
    sbt ~test
else
    echo "Available options:"
    echo "  --unit, -u         Run unit tests only"
    echo "  --integration, -i  Run all tests (unit + integration)"
    echo "  --watch, -w        Run tests in watch mode"
    echo ""
    echo "Running unit tests by default..."
    echo ""
    sbt core/test
    echo ""
    echo "✅ Unit tests completed!"
    echo ""
    echo "💡 Tip: Run './test.sh --integration' for full test suite"
fi
