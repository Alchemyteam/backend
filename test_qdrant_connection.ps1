# 测试 Qdrant 连接和集合状态

Write-Host "=== 测试 Qdrant 连接 ===" -ForegroundColor Green

# 1. 检查 Qdrant 健康状态（尝试多个端点）
Write-Host "`n1. 检查 Qdrant 健康状态..." -ForegroundColor Yellow

$healthEndpoints = @("/", "/healthz", "/health")
$healthFound = $false

foreach ($endpoint in $healthEndpoints) {
    try {
        $health = Invoke-WebRequest -Uri "http://localhost:6333$endpoint" -Method GET -ErrorAction Stop
        Write-Host "Qdrant 健康状态 ($endpoint): $($health.StatusCode)" -ForegroundColor Green
        Write-Host "响应: $($health.Content)" -ForegroundColor Cyan
        $healthFound = $true
        break
    } catch {
        Write-Host "端点 $endpoint 不可用: $($_.Exception.Message)" -ForegroundColor Yellow
    }
}

if (-not $healthFound) {
    Write-Host "`n错误: Qdrant 服务可能未运行或无法访问" -ForegroundColor Red
    Write-Host "请确保 Qdrant 正在运行:" -ForegroundColor Yellow
    Write-Host "  docker run -d -p 6333:6333 qdrant/qdrant" -ForegroundColor Yellow
    Write-Host "或检查 Qdrant 是否在其他端口运行" -ForegroundColor Yellow
    exit
}

# 2. 检查集合是否存在
Write-Host "`n2. 检查集合是否存在..." -ForegroundColor Yellow
try {
    $collection = Invoke-WebRequest -Uri "http://localhost:6333/collections/sales_data_vectors" -Method GET
    Write-Host "集合状态: $($collection.StatusCode)" -ForegroundColor Cyan
    Write-Host "集合信息:" -ForegroundColor Cyan
    $collection.Content | ConvertFrom-Json | ConvertTo-Json -Depth 10
} catch {
    Write-Host "集合不存在或无法访问" -ForegroundColor Yellow
    Write-Host "错误: $($_.Exception.Message)" -ForegroundColor Red
}

# 3. 列出所有集合
Write-Host "`n3. 列出所有集合..." -ForegroundColor Yellow
try {
    $collections = Invoke-WebRequest -Uri "http://localhost:6333/collections" -Method GET
    Write-Host "所有集合:" -ForegroundColor Cyan
    $collections.Content | ConvertFrom-Json | ConvertTo-Json -Depth 10
} catch {
    Write-Host "无法获取集合列表" -ForegroundColor Red
}

Write-Host "`n=== 测试完成 ===" -ForegroundColor Green

