#!/bin/bash

# Contextual Retrieval CLI Client
# Usage: ./contextual-retrieval.sh [command] [options]

set -e

# Configuration
API_BASE_URL="${API_BASE_URL:-http://localhost:8080/api/documents}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Helper functions
print_usage() {
  cat << EOF
${BLUE}Contextual Retrieval CLI Client${NC}

Usage: ./contextual-retrieval.sh [command] [options]

Commands:
  upload    Upload a document
  query     Query the document database
  help      Show this help message

Examples:
  # Upload a document
  ./contextual-retrieval.sh upload --file /path/to/document.pdf --name "My Document"

  # Query the database
  ./contextual-retrieval.sh query --text "What is the main topic?"

  # Set custom API URL
  API_BASE_URL=http://localhost:9000/api/documents ./contextual-retrieval.sh query --text "Your query"

EOF
}

print_error() {
  echo -e "${RED}✗ Error: $1${NC}" >&2
}

print_success() {
  echo -e "${GREEN}✓ $1${NC}"
}

print_info() {
  echo -e "${BLUE}ℹ $1${NC}"
}

# Upload document
upload_document() {
  local file=""
  local name=""

  # Parse arguments
  while [[ $# -gt 0 ]]; do
    case $1 in
      --file)
        file="$2"
        shift 2
        ;;
      --name)
        name="$2"
        shift 2
        ;;
      *)
        print_error "Unknown option: $1"
        exit 1
        ;;
    esac
  done

  # Validate arguments
  if [[ -z "$file" ]]; then
    print_error "Missing required option: --file"
    exit 1
  fi

  if [[ -z "$name" ]]; then
    print_error "Missing required option: --name"
    exit 1
  fi

  if [[ ! -f "$file" ]]; then
    print_error "File not found: $file"
    exit 1
  fi

  print_info "Uploading document: $name"
  print_info "File: $file"
  print_info "API URL: $API_BASE_URL"

  response=$(curl -s -w "\n%{http_code}" -X POST \
    -F "file=@$file" \
    -F "documentName=$name" \
    "$API_BASE_URL")

  http_code=$(echo "$response" | tail -n 1)
  body=$(echo "$response" | sed '$d')

  if [[ "$http_code" == "201" ]] || [[ "$http_code" == "200" ]]; then
    print_success "Document uploaded successfully"
    echo "$body"
  else
    print_error "Failed to upload document (HTTP $http_code)"
    echo "$body"
    exit 1
  fi
}

# Query documents
query_documents() {
  local query_text=""

  # Parse arguments
  while [[ $# -gt 0 ]]; do
    case $1 in
      --text)
        query_text="$2"
        shift 2
        ;;
      *)
        print_error "Unknown option: $1"
        exit 1
        ;;
    esac
  done

  # Validate arguments
  if [[ -z "$query_text" ]]; then
    print_error "Missing required option: --text"
    exit 1
  fi

  print_info "Processing query: $query_text"
  print_info "API URL: $API_BASE_URL/query"

  response=$(curl -s -w "\n%{http_code}" -X POST \
    -G "$API_BASE_URL/query" \
    --data-urlencode "query=$query_text")

  http_code=$(echo "$response" | tail -n 1)
  body=$(echo "$response" | sed '$d')

  if [[ "$http_code" == "200" ]]; then
    print_success "Query processed successfully"
    echo ""
    echo -e "${YELLOW}Answer:${NC}"
    echo "$body"
  else
    print_error "Failed to process query (HTTP $http_code)"
    echo "$body"
    exit 1
  fi
}

# Main logic
main() {
  if [[ $# -eq 0 ]]; then
    print_usage
    exit 0
  fi

  command="$1"
  shift

  case "$command" in
    upload)
      upload_document "$@"
      ;;
    query)
      query_documents "$@"
      ;;
    help|-h|--help)
      print_usage
      ;;
    *)
      print_error "Unknown command: $command"
      print_usage
      exit 1
      ;;
  esac
}

main "$@"
