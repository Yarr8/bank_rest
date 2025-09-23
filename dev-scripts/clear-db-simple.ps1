# скрипт, убирающий тестовые данные из БД
# очищает все таблицы, только оставляет админа в USERS, чтобы было проще тестировать

Write-Host "=== Simple Database Cleanup ===" -ForegroundColor Green
Write-Host ""

# Check if DEV containers running
Write-Host "Checking containers..." -ForegroundColor Yellow

$postgresRunning = docker ps --filter "name=bankcards-postgres-dev" --format "table {{.Names}}" | Select-String "bankcards-postgres-dev"

if (-not $postgresRunning) {
    Write-Host "PostgreSQL container is not running. Please start it first:" -ForegroundColor Red
    Write-Host "docker-compose -f docker-compose.dev.yml up -d" -ForegroundColor Yellow
    exit 1
}

Write-Host "PostgreSQL container is running. Cleaning database..." -ForegroundColor Green

# Clean tables
Write-Host "Disabling foreign key checks..." -ForegroundColor Yellow
docker exec bankcards-postgres-dev psql -U bankcards -d bankcards -c "SET session_replication_role = replica;"

Write-Host "Cleaning data (preserving admin users)..." -ForegroundColor Yellow
docker exec bankcards-postgres-dev psql -U bankcards -d bankcards -c "TRUNCATE TABLE card_block_requests CASCADE;"
docker exec bankcards-postgres-dev psql -U bankcards -d bankcards -c "TRUNCATE TABLE transactions CASCADE;"
docker exec bankcards-postgres-dev psql -U bankcards -d bankcards -c "TRUNCATE TABLE cards CASCADE;"
docker exec bankcards-postgres-dev psql -U bankcards -d bankcards -c "DELETE FROM users WHERE role = 'USER';"

Write-Host "Re-enabling foreign key checks..." -ForegroundColor Yellow
docker exec bankcards-postgres-dev psql -U bankcards -d bankcards -c "SET session_replication_role = DEFAULT;"

Write-Host "Resetting sequences..." -ForegroundColor Yellow
# Reset sequence. For USERS setting first available ID as Admin should stay in table
Write-Host "Setting users sequence to next available ID..." -ForegroundColor Yellow
docker exec bankcards-postgres-dev psql -U bankcards -d bankcards -c "SELECT setval('users_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM users), false);"
docker exec bankcards-postgres-dev psql -U bankcards -d bankcards -c "SELECT setval('cards_id_seq', 1, false);"
docker exec bankcards-postgres-dev psql -U bankcards -d bankcards -c "SELECT setval('transactions_id_seq', 1, false);"
docker exec bankcards-postgres-dev psql -U bankcards -d bankcards -c "SELECT setval('card_block_requests_id_seq', 1, false);"

Write-Host ""
Write-Host "Database cleaned successfully!" -ForegroundColor Green
Write-Host "All test data removed" -ForegroundColor Green
Write-Host "Admin users preserved" -ForegroundColor Green
Write-Host "Sequences reset" -ForegroundColor Green
Write-Host ""
Write-Host "You can now run your tests with clean database." -ForegroundColor White
