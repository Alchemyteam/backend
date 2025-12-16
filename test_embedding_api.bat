@echo off
REM Windows 批处理脚本：测试 Embedding API

echo === 测试 Embedding API ===

echo.
echo 1. 生成所有记录的 embedding_text...
curl.exe -X POST "http://localhost:8000/api/admin/embedding/generate-all?batchSize=50"

echo.
echo 2. 生成向量并存储到 Qdrant...
curl.exe -X POST "http://localhost:8000/api/admin/embedding/generate-vectors?batchSize=50"

echo.
echo 3. 测试向量搜索...
curl.exe -X GET "http://localhost:8000/api/admin/embedding/search?query=safety%20helmet&topK=10"

echo.
echo === 测试完成 ===
pause

