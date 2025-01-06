#!/bin/bash

BASE_URL="http://localhost:10001"

# Extract token from JSON response
extract_token() {
  echo $1 | jq -r .token
}

echo "1) Register user testuser1"
REGISTER_RESPONSE1=$(curl -s -X POST $BASE_URL/register \
    -H "Content-Type: application/json" \
    -d '{"username": "testuser1", "password": "password123"}')
echo "Register Response: $REGISTER_RESPONSE1"
echo -e "\n"

echo "2) Register user testuser2"
REGISTER_RESPONSE2=$(curl -s -X POST $BASE_URL/register \
    -H "Content-Type: application/json" \
    -d '{"username": "testuser2", "password": "password456"}')
echo "Register Response: $REGISTER_RESPONSE2"
echo -e "\n"

echo "3) Login user testuser1"
LOGIN_RESPONSE1=$(curl -s -X POST $BASE_URL/login \
    -H "Content-Type: application/json" \
    -d '{"username": "testuser1", "password": "password123"}')
echo "Login Response for testuser1: $LOGIN_RESPONSE1"
TOKEN1=$(extract_token "$LOGIN_RESPONSE1")
echo "Extracted Token1: $TOKEN1"
if [[ -z "$TOKEN1" || "$TOKEN1" == "null" ]]; then
  echo "Failed to obtain token for testuser1"
  exit 1
fi
echo -e "\n"

echo "4) Login user testuser2"
LOGIN_RESPONSE2=$(curl -s -X POST $BASE_URL/login \
    -H "Content-Type: application/json" \
    -d '{"username": "testuser2", "password": "password456"}')
echo "Login Response for testuser2: $LOGIN_RESPONSE2"
TOKEN2=$(extract_token "$LOGIN_RESPONSE2")
echo "Extracted Token2: $TOKEN2"
if [[ -z "$TOKEN2" || "$TOKEN2" == "null" ]]; then
  echo "Failed to obtain token for testuser2"
  exit 1
fi
echo -e "\n"

echo "5) Acquire package for testuser1"
PACKAGE_RESPONSE1=$(curl -s -X POST $BASE_URL/package \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $TOKEN1" \
    -d '{"username": "testuser1"}')
echo "Package Response for testuser1: $PACKAGE_RESPONSE1"
echo -e "\n"

echo "6) Acquire package for testuser2"
PACKAGE_RESPONSE2=$(curl -s -X POST $BASE_URL/package \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $TOKEN2" \
    -d '{"username": "testuser2"}')
echo "Package Response for testuser2: $PACKAGE_RESPONSE2"
echo -e "\n"

echo "7) Verify acquired cards for testuser1"
CARDS_RESPONSE1=$(curl -s -X GET "$BASE_URL/cards" \
    -H "Authorization: Bearer $TOKEN1")
echo "Cards for testuser1: $CARDS_RESPONSE1"
echo -e "\n"

echo "8) Verify acquired cards for testuser2"
CARDS_RESPONSE2=$(curl -s -X GET "$BASE_URL/cards" \
    -H "Authorization: Bearer $TOKEN2")
echo "Cards for testuser2: $CARDS_RESPONSE2"
echo -e "\n"
