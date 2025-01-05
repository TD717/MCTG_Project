#!/bin/bash

BASE_URL="http://localhost:10001"

# Token extraction using jq
extract_token() {
  echo $1 | jq -r .token
}

echo "1) Register user testuser1"
curl -X POST $BASE_URL/register \
    -H "Content-Type: application/json" \
    -d '{"username": "testuser1", "password": "password123"}'
echo -e "\n"

echo "2) Register user testuser2"
curl -X POST $BASE_URL/register \
    -H "Content-Type: application/json" \
    -d '{"username": "testuser2", "password": "password456"}'
echo -e "\n"

echo "3) Login user testuser1"
LOGIN_RESPONSE1=$(curl -X POST $BASE_URL/login \
    -H "Content-Type: application/json" \
    -d '{"username": "testuser1", "password": "password123"}')
TOKEN1=$(extract_token "$LOGIN_RESPONSE1")
echo "Token1: $TOKEN1"
echo -e "\n"

echo "4) Login user testuser2"
LOGIN_RESPONSE2=$(curl -X POST $BASE_URL/login \
    -H "Content-Type: application/json" \
    -d '{"username": "testuser2", "password": "password456"}')
TOKEN2=$(extract_token "$LOGIN_RESPONSE2")
echo "Token2: $TOKEN2"
echo -e "\n"

echo "5) Acquire package for testuser1"
curl -X POST $BASE_URL/package \
    -H "Content-Type: application/json" \
    -d "{\"username\": \"testuser1\", \"token\": \"$TOKEN1\"}"
echo -e "\n"

echo "6) Acquire package for testuser2"
curl -X POST $BASE_URL/package \
    -H "Content-Type: application/json" \
    -d "{\"username\": \"testuser2\", \"token\": \"$TOKEN2\"}"
echo -e "\n"
