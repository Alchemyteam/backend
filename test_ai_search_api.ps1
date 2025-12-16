# AI Search API Test Script
# Tests the integrated vector search + AI Q&A functionality

$baseUrl = "http://localhost:8000/api/chat/message"
$token = "YOUR_TOKEN_HERE"  # Replace with your actual token

# Test queries (English only)
$testQueries = @(
    "safety helmet",
    "protective gloves",
    "steel pipe",
    "welding machine",
    "filter cartridge",
    "what safety equipment do you have",
    "show me protective gear",
    "find measurement instruments",
    "steel products for construction",
    "FLUKE products",
    "Site Safety Equipment",
    "safety helmet with visor",
    "high quality safety helmet",
    "LEAKAGE CURRENT CLAMP METER",
    "equipment for construction safety"
)

Write-Host "=== AI Search API Test ===" -ForegroundColor Green
Write-Host "Testing vector search + AI Q&A integration`n" -ForegroundColor Yellow

foreach ($query in $testQueries) {
    Write-Host "`n[Test] Query: '$query'" -ForegroundColor Cyan
    
    $body = @{
        message = $query
    } | ConvertTo-Json
    
    try {
        $response = Invoke-WebRequest -Uri $baseUrl `
            -Method POST `
            -Headers @{
                "Authorization" = "Bearer $token"
                "Content-Type" = "application/json"
            } `
            -Body $body `
            -ErrorAction Stop
        
        $result = $response.Content | ConvertFrom-Json
        
        Write-Host "Status: $($response.StatusCode)" -ForegroundColor Green
        Write-Host "AI Response: $($result.response)" -ForegroundColor White
        
        if ($result.tableData) {
            Write-Host "Table Data: Found $($result.tableData.rows.Count) products" -ForegroundColor Yellow
            if ($result.tableData.rows.Count -gt 0) {
                Write-Host "First Product: $($result.tableData.rows[0].'Item Name')" -ForegroundColor Gray
            }
        } else {
            Write-Host "No table data returned" -ForegroundColor Gray
        }
        
        Start-Sleep -Seconds 1  # Rate limiting delay
        
    } catch {
        Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
        if ($_.Exception.Response) {
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $responseBody = $reader.ReadToEnd()
            Write-Host "Response: $responseBody" -ForegroundColor Red
        }
    }
    
    Write-Host "---" -ForegroundColor DarkGray
}

Write-Host "`n=== Test Complete ===" -ForegroundColor Green

