# 测试向量搜索功能

Write-Host "=== 测试向量搜索 ===" -ForegroundColor Green

# 测试不同的搜索查询
$queries = @(
    "safety helmet",
    "protective equipment",
    "construction material",
    "steel pipe",
    "welding machine"
)

foreach ($query in $queries) {
    Write-Host "`n搜索: '$query'" -ForegroundColor Yellow
    $encodedQuery = [System.Web.HttpUtility]::UrlEncode($query)
    
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8000/api/admin/embedding/search?query=$encodedQuery&topK=5" -Method GET
        $result = $response.Content | ConvertFrom-Json
        
        Write-Host "找到 $($result.count) 个相似产品" -ForegroundColor Cyan
        Write-Host "产品 IDs: $($result.results -join ', ')" -ForegroundColor Cyan
    } catch {
        Write-Host "搜索失败: $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host "`n=== 测试完成 ===" -ForegroundColor Green

