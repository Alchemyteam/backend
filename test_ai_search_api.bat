@echo off
REM AI Search API Test Script (Batch version)
REM Tests the integrated vector search + AI Q&A functionality

set BASE_URL=http://localhost:8000/api/chat/message
set TOKEN=YOUR_TOKEN_HERE

echo === AI Search API Test ===
echo Testing vector search + AI Q&A integration
echo.

echo [Test 1] Query: "safety helmet"
curl.exe -X POST "%BASE_URL%" ^
  -H "Authorization: Bearer %TOKEN%" ^
  -H "Content-Type: application/json" ^
  -d "{\"message\": \"safety helmet\"}"
echo.
echo.

echo [Test 2] Query: "protective gloves"
curl.exe -X POST "%BASE_URL%" ^
  -H "Authorization: Bearer %TOKEN%" ^
  -H "Content-Type: application/json" ^
  -d "{\"message\": \"protective gloves\"}"
echo.
echo.

echo [Test 3] Query: "steel pipe"
curl.exe -X POST "%BASE_URL%" ^
  -H "Authorization: Bearer %TOKEN%" ^
  -H "Content-Type: application/json" ^
  -d "{\"message\": \"steel pipe\"}"
echo.
echo.

echo [Test 4] Query: "what safety equipment do you have"
curl.exe -X POST "%BASE_URL%" ^
  -H "Authorization: Bearer %TOKEN%" ^
  -H "Content-Type: application/json" ^
  -d "{\"message\": \"what safety equipment do you have\"}"
echo.
echo.

echo [Test 5] Query: "FLUKE products"
curl.exe -X POST "%BASE_URL%" ^
  -H "Authorization: Bearer %TOKEN%" ^
  -H "Content-Type: application/json" ^
  -d "{\"message\": \"FLUKE products\"}"
echo.
echo.

echo === Test Complete ===

