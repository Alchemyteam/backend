# PowerShell 脚本：测试 Embedding API

Write-Host "=== 测试 Embedding API ===" -ForegroundColor Green

# 1. 生成所有记录的 embedding_text
Write-Host "`n1. 生成所有记录的 embedding_text..." -ForegroundColor Yellow
$response1 = Invoke-WebRequest -Uri "http://localhost:8000/api/admin/embedding/generate-all?batchSize=50" -Method POST -ContentType "application/json"
Write-Host "响应状态码: $($response1.StatusCode)" -ForegroundColor Cyan
Write-Host "响应内容:" -ForegroundColor Cyan
$response1.Content | ConvertFrom-Json | ConvertTo-Json -Depth 10

# 2. 生成向量并存储到 Qdrant
Write-Host "`n2. 生成向量并存储到 Qdrant..." -ForegroundColor Yellow
$response2 = Invoke-WebRequest -Uri "http://localhost:8000/api/admin/embedding/generate-vectors?batchSize=50" -Method POST -ContentType "application/json"
Write-Host "响应状态码: $($response2.StatusCode)" -ForegroundColor Cyan
Write-Host "响应内容:" -ForegroundColor Cyan
$response2.Content | ConvertFrom-Json | ConvertTo-Json -Depth 10

# 3. 测试向量搜索
Write-Host "`n3. 测试向量搜索..." -ForegroundColor Yellow
$query = "safety helmet"
$encodedQuery = [System.Web.HttpUtility]::UrlEncode($query)
$response3 = Invoke-WebRequest -Uri "http://localhost:8000/api/admin/embedding/search?query=$encodedQuery&topK=10" -Method GET
Write-Host "响应状态码: $($response3.StatusCode)" -ForegroundColor Cyan
Write-Host "响应内容:" -ForegroundColor Cyan
$response3.Content | ConvertFrom-Json | ConvertTo-Json -Depth 10

Write-Host "`n=== 测试完成 ===" -ForegroundColor Green

