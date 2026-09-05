#!/bin/bash

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Print banner
echo -e "${BLUE}===============================================${NC}"
echo -e "${BLUE}🛡️  Trivy DevSecOps Security Scanner${NC}"
echo -e "${BLUE}===============================================${NC}"

# Find project root (directory where build-docker-images.sh resides)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Check if docker is installed
if ! command -v docker &> /dev/null; then
    echo -e "${RED}[ERROR] Docker is required to run the scanner without a local Trivy installation.${NC}"
    exit 1
fi

echo -e "${GREEN}[INFO]${NC} Project root directory: $PROJECT_ROOT"
echo -e "${GREEN}[INFO]${NC} Running Trivy scan via Docker..."

# Pull latest Trivy image
docker pull aquasec/trivy:latest > /dev/null

# 1. Scan filesystem for vulnerabilities and secrets
echo -e "\n${YELLOW}🔍 1. Scanning Filesystem (Vulnerabilities & Secrets)...${NC}"
docker run --rm \
  -v "$PROJECT_ROOT":/workspace \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v trivy-cache:/root/.cache \
  aquasec/trivy:latest fs \
  --scanners vuln,secret \
  --severity HIGH,CRITICAL \
  /workspace

# 2. Scan Infrastructure as Code (Kubernetes Base Manifests & Docker Compose)
echo -e "\n${YELLOW}🔍 2. Scanning Infrastructure as Code (K8s & Compose Misconfigurations)...${NC}"
docker run --rm \
  -v "$PROJECT_ROOT":/workspace \
  -v trivy-cache:/root/.cache \
  aquasec/trivy:latest config \
  --severity HIGH,CRITICAL \
  /workspace

echo -e "\n${GREEN}===============================================${NC}"
echo -e "${GREEN}✅ Security scan completed!${NC}"
echo -e "${GREEN}===============================================${NC}"
