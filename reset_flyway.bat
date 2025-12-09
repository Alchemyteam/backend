@echo off
REM ============================================
REM 重置 Flyway 迁移历史
REM ============================================

echo 正在连接数据库并删除 flyway_schema_history 表...

mysql -u root -pallinton -e "DROP TABLE IF EXISTS ecoschema.flyway_schema_history;"

if %ERRORLEVEL% EQU 0 (
    echo.
    echo 成功！flyway_schema_history 表已删除。
    echo 现在可以重启应用，Flyway 会重新执行所有迁移。
) else (
    echo.
    echo 错误：无法连接到数据库或执行 SQL。
    echo 请手动在 MySQL 中执行：DROP TABLE IF EXISTS ecoschema.flyway_schema_history;
)

pause

