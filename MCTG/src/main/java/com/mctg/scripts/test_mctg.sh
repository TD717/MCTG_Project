#!/bin/bash

BASE_URL="http://localhost:10001"

# Extract token from JSON response
extract_token() {
  echo "$1" | jq -r .token
}

# Register users
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

# Login users
echo "3) Login user testuser1"
LOGIN_RESPONSE1=$(curl -s -X POST $BASE_URL/login \
    -H "Content-Type: application/json" \
    -d '{"username": "testuser1", "password": "password123"}')
echo "Login Response for testuser1: $LOGIN_RESPONSE1"
TOKEN1=$(extract_token "$LOGIN_RESPONSE1")
echo "Extracted Token1: $TOKEN1"
echo -e "\n"

echo "4) Login user testuser2"
LOGIN_RESPONSE2=$(curl -s -X POST $BASE_URL/login \
    -H "Content-Type: application/json" \
    -d '{"username": "testuser2", "password": "password456"}')
echo "Login Response for testuser2: $LOGIN_RESPONSE2"
TOKEN2=$(extract_token "$LOGIN_RESPONSE2")
echo "Extracted Token2: $TOKEN2"
echo -e "\n"

# Acquire packages for both users
echo "5) Acquire package for testuser1"
PACKAGE_RESPONSE1=$(curl -s -X POST $BASE_URL/package \
    -H "Authorization: Bearer $TOKEN1" \
    -H "Content-Type: application/json" \
    -d '{}')
echo "Package Response for testuser1: $PACKAGE_RESPONSE1"
echo -e "\n"

echo "6) Acquire package for testuser2"
PACKAGE_RESPONSE2=$(curl -s -X POST $BASE_URL/package \
    -H "Authorization: Bearer $TOKEN2" \
    -H "Content-Type: application/json" \
    -d '{}')
echo "Package Response for testuser2: $PACKAGE_RESPONSE2"
echo -e "\n"

# Update password only for testuser2
echo "8) Update password for testuser2"
UPDATE_RESPONSE2=$(curl -s -X PUT $BASE_URL/users \
    -H "Authorization: Bearer $TOKEN2" \
    -H "Content-Type: application/json" \
    -d '{
      "username": "testuser2",
      "password": "newpassword456",
      "token": "'"$TOKEN2"'"
    }')
echo "Update Response for testuser2: $UPDATE_RESPONSE2"
echo -e "\n"

# Re-login with new password for testuser2
echo "10) Re-login with updated password for testuser2"
LOGIN_RESPONSE_UPDATED2=$(curl -s -X POST $BASE_URL/login \
    -H "Content-Type: application/json" \
    -d '{"username": "testuser2", "password": "newpassword456"}')
echo "Re-login Response for testuser2: $LOGIN_RESPONSE_UPDATED2"
TOKEN2_UPDATED=$(extract_token "$LOGIN_RESPONSE_UPDATED2")
echo "Extracted Updated Token2: $TOKEN2_UPDATED"
echo -e "\n"

#!/bin/bash

BASE_URL="http://localhost:10001"

# Retrieve deck for testuser1
echo "12) Retrieve deck for testuser1"
GET_DECK_RESPONSE1=$(curl -s -X GET $BASE_URL/deck \
    -H "Authorization: Bearer $TOKEN1" \
    -H "Content-Type: application/json" \
    -d '{"username": "testuser1"}')
echo "Retrieve Deck Response for testuser1: $GET_DECK_RESPONSE1"
echo -e "\n"

# Retrieve deck for testuser2
echo "13) Retrieve deck for testuser2"
GET_DECK_RESPONSE2=$(curl -s -X GET $BASE_URL/deck \
    -H "Authorization: Bearer $TOKEN2" \
    -H "Content-Type: application/json" \
    -d '{"username": "testuser2"}')
echo "Retrieve Deck Response for testuser2: $GET_DECK_RESPONSE2"
echo -e "\n"

# Show scoreboard
echo "14) Retrieve scoreboard"
SCOREBOARD_RESPONSE=$(curl -s -X GET $BASE_URL/scoreboard \
    -H "Authorization: Bearer $TOKEN1" \
    -H "Content-Type: application/json")
echo "Scoreboard Response: $SCOREBOARD_RESPONSE"
echo -e "\n"
