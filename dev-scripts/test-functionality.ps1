# скрипт для E2E тестирования без поднятия лишних зависимостей
# написал для себя, чтобы быть на 100% уверенным в работоспособности функционала
# запускать после поднятия docker-compose.dev.yml
# работает ТОЛЬКО с DEV окружением
# для сброса БД в исходное состояние есть дополнительный скрипт clear-db-simple.ps1

$baseUrl = "http://localhost:8080/api"
$headers = @{}

Write-Host "=== Bank Cards API Functionality Test ===" -ForegroundColor Green
Write-Host ""

# Test 1: Login as admin first
Write-Host "1. Testing admin login..." -ForegroundColor Yellow

$adminLoginData = @{
    username = "admin"
    password = "admin123"
} | ConvertTo-Json

try {
    $adminLogin = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method POST -Body $adminLoginData -ContentType "application/json"
    $adminHeaders = @{ "Authorization" = "Bearer $($adminLogin.token)" }
    Write-Host "Admin logged in successfully" -ForegroundColor Green
} catch {
    Write-Host "Admin login failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Cannot continue without admin access" -ForegroundColor Red
    exit 1
}

Write-Host ""

# Test 2: Create test users (admin only)
Write-Host "2. Testing user creation by admin..." -ForegroundColor Yellow

$user1 = @{
    username = "testuser1"
    password = "password123"
    role = "USER"
} | ConvertTo-Json

$user2 = @{
    username = "testuser2"
    password = "password123"
    role = "USER"
} | ConvertTo-Json

try {
    $reg1 = Invoke-RestMethod -Uri "$baseUrl/admin/users" -Method POST -Body $user1 -ContentType "application/json" -Headers $adminHeaders
    $userId1 = $reg1.id
    Write-Host "User1 created successfully by admin (ID: $userId1)" -ForegroundColor Green
} catch {
    Write-Host "User1 creation failed: $($_.Exception.Message)" -ForegroundColor Red
    $userId1 = $null
}

try {
    $reg2 = Invoke-RestMethod -Uri "$baseUrl/admin/users" -Method POST -Body $user2 -ContentType "application/json" -Headers $adminHeaders
    $userId2 = $reg2.id
    Write-Host "User2 created successfully by admin (ID: $userId2)" -ForegroundColor Green
} catch {
    Write-Host "User2 creation failed: $($_.Exception.Message)" -ForegroundColor Red
    $userId2 = $null
}

Write-Host ""

# Test 3: Login as created users
Write-Host "3. Testing user login..." -ForegroundColor Yellow

$loginData1 = @{
    username = "testuser1"
    password = "password123"
} | ConvertTo-Json

$loginData2 = @{
    username = "testuser2"
    password = "password123"
} | ConvertTo-Json

try {
    $login1 = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method POST -Body $loginData1 -ContentType "application/json"
    $headers1 = @{ "Authorization" = "Bearer $($login1.token)" }
    Write-Host "User1 logged in successfully" -ForegroundColor Green
} catch {
    Write-Host "User1 login failed: $($_.Exception.Message)" -ForegroundColor Red
}

try {
    $login2 = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method POST -Body $loginData2 -ContentType "application/json"
    $headers2 = @{ "Authorization" = "Bearer $($login2.token)" }
    Write-Host "User2 logged in successfully" -ForegroundColor Green
} catch {
    Write-Host "User2 login failed: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# Test 4: Create cards for users
Write-Host "4. Testing card creation..." -ForegroundColor Yellow

$cardData1 = @{
    userId = $userId1
    cardNumber = "1234567890123456"
    expiryDate = "2025-12-31"
    owner = "John Doe"
    balance = 1000.00
} | ConvertTo-Json

$cardData2 = @{
    userId = $userId2
    cardNumber = "9876543210987654"
    expiryDate = "2026-06-30"
    owner = "Jane Doe"
    balance = 500.00
} | ConvertTo-Json

if ($userId1 -ne $null) {
    try {
        $card1 = Invoke-RestMethod -Uri "$baseUrl/admin/cards" -Method POST -Body $cardData1 -ContentType "application/json" -Headers $adminHeaders
        $cardId1 = $card1.id
        Write-Host "Card1 created successfully for User1 (ID: $cardId1)" -ForegroundColor Green
    } catch {
        Write-Host "Card1 creation failed: $($_.Exception.Message)" -ForegroundColor Red
        $cardId1 = $null
    }
} else {
    Write-Host "Cannot create Card1 - User1 was not created" -ForegroundColor Red
    $cardId1 = $null
}

if ($userId2 -ne $null) {
    try {
        $card2 = Invoke-RestMethod -Uri "$baseUrl/admin/cards" -Method POST -Body $cardData2 -ContentType "application/json" -Headers $adminHeaders
        $cardId2 = $card2.id
        Write-Host "Card2 created successfully for User2 (ID: $cardId2)" -ForegroundColor Green
    } catch {
        Write-Host "Card2 creation failed: $($_.Exception.Message)" -ForegroundColor Red
        $cardId2 = $null
    }
} else {
    Write-Host "Cannot create Card2 - User2 was not created" -ForegroundColor Red
    $cardId2 = $null
}

Write-Host ""

# Test 5: Get user's cards
Write-Host "5. Testing get user cards..." -ForegroundColor Yellow

try {
    $userCardsResponse1 = Invoke-RestMethod -Uri "$baseUrl/cards" -Method GET -Headers $headers1
    $userCards1 = $userCardsResponse1.content
    Write-Host "User1 cards retrieved: $($userCards1.Count) cards" -ForegroundColor Green
    if ($userCards1.Count -gt 0) {
        Write-Host "  Card numbers: $($userCards1.cardNumber -join ', ')" -ForegroundColor Cyan
    }
} catch {
    Write-Host "Failed to get User1 cards: $($_.Exception.Message)" -ForegroundColor Red
}

try {
    $userCardsResponse2 = Invoke-RestMethod -Uri "$baseUrl/cards" -Method GET -Headers $headers2
    $userCards2 = $userCardsResponse2.content
    Write-Host "User2 cards retrieved: $($userCards2.Count) cards" -ForegroundColor Green
    if ($userCards2.Count -gt 0) {
        Write-Host "  Card numbers: $($userCards2.cardNumber -join ', ')" -ForegroundColor Cyan
    }
} catch {
    Write-Host "Failed to get User2 cards: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# Test 6: Create second card for User1 (to check internal transfers)
Write-Host "6. Creating second card for User1..." -ForegroundColor Yellow

$cardData3 = @{
    userId = $userId1
    cardNumber = "1111222233334444"
    expiryDate = "2026-12-30"
    owner = "John Doe Second Card"
    balance = 2000.00
} | ConvertTo-Json

if ($userId1 -ne $null) {
    try {
        $card3 = Invoke-RestMethod -Uri "$baseUrl/admin/cards" -Method POST -Body $cardData3 -ContentType "application/json" -Headers $adminHeaders
        $cardId3 = $card3.id
        Write-Host "Second card created successfully for User1 (ID: $cardId3)" -ForegroundColor Green
    } catch {
        Write-Host "Second card creation failed: $($_.Exception.Message)" -ForegroundColor Red
        $cardId3 = $null
    }
} else {
    Write-Host "Cannot create second card - User1 was not created" -ForegroundColor Red
    $cardId3 = $null
}

Write-Host ""

# Test 7: Create transactions (internal transfer between User1's cards)
Write-Host "7. Testing transaction creation (internal transfer)..." -ForegroundColor Yellow

if ($cardId1 -ne $null -and $cardId3 -ne $null) {
    try {
        $transactionData = @{
            fromCardNumber = "1234567890123456"
            toCardNumber = "1111222233334444"
            amount = 100.50
            description = "Internal transfer between User1 cards"
        } | ConvertTo-Json

        $transaction = Invoke-RestMethod -Uri "$baseUrl/transactions" -Method POST -Body $transactionData -ContentType "application/json" -Headers $headers1
        Write-Host "Internal transaction created successfully" -ForegroundColor Green
    } catch {
        Write-Host "Internal transaction creation failed: $($_.Exception.Message)" -ForegroundColor Red
    }
} else {
    Write-Host "Cannot create transaction - Need at least 2 cards for User1" -ForegroundColor Red
}

Write-Host ""

# Test 8: Get transactions
Write-Host "8. Testing get transactions..." -ForegroundColor Yellow

try {
    $transactions1 = Invoke-RestMethod -Uri "$baseUrl/transactions" -Method GET -Headers $headers1
    Write-Host "User1 transactions retrieved: $($transactions1.Count) transactions" -ForegroundColor Green
} catch {
    Write-Host "Failed to get User1 transactions: $($_.Exception.Message)" -ForegroundColor Red
}

try {
    $transactions2 = Invoke-RestMethod -Uri "$baseUrl/transactions" -Method GET -Headers $headers2
    Write-Host "User2 transactions retrieved: $($transactions2.Count) transactions" -ForegroundColor Green
} catch {
    Write-Host "Failed to get User2 transactions: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# Test 9: Request card block
Write-Host "9. Testing card block request..." -ForegroundColor Yellow

$blockRequestData = @{
    cardId = $cardId1
    reason = "Lost card"
} | ConvertTo-Json

if ($cardId1 -ne $null) {
    try {
        $blockRequest = Invoke-RestMethod -Uri "$baseUrl/cards/$cardId1/request-block" -Method POST -Body $blockRequestData -ContentType "application/json" -Headers $headers1
        Write-Host "Card block request created successfully" -ForegroundColor Green
    } catch {
        Write-Host "Card block request failed: $($_.Exception.Message)" -ForegroundColor Red
    }
} else {
    Write-Host "Cannot create block request - Card1 was not created" -ForegroundColor Red
}

Write-Host ""

# Test 10: Admin endpoints
Write-Host "10. Testing admin endpoints..." -ForegroundColor Yellow

try {
    $allUsers = Invoke-RestMethod -Uri "$baseUrl/admin/users" -Method GET -Headers $adminHeaders
    Write-Host "Admin retrieved all users: $($allUsers.Count) users" -ForegroundColor Green
} catch {
    Write-Host "Failed to get all users: $($_.Exception.Message)" -ForegroundColor Red
}

try {
    $allCards = Invoke-RestMethod -Uri "$baseUrl/admin/cards" -Method GET -Headers $adminHeaders
    Write-Host "Admin retrieved all cards: $($allCards.Count) cards" -ForegroundColor Green
} catch {
    Write-Host "Failed to get all cards: $($_.Exception.Message)" -ForegroundColor Red
}

try {
    $allTransactions = Invoke-RestMethod -Uri "$baseUrl/admin/transactions" -Method GET -Headers $adminHeaders
    Write-Host "Admin retrieved all transactions: $($allTransactions.Count) transactions" -ForegroundColor Green
} catch {
    Write-Host "Failed to get all transactions: $($_.Exception.Message)" -ForegroundColor Red
}

try {
    $blockRequests = Invoke-RestMethod -Uri "$baseUrl/admin/card-block-requests" -Method GET -Headers $adminHeaders
    Write-Host "Admin retrieved block requests: $($blockRequests.Count) requests" -ForegroundColor Green
} catch {
    Write-Host "Failed to get block requests: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# Test 11: Approve block request (if exists)
Write-Host "11. Testing block request approval..." -ForegroundColor Yellow

try {
    $blockRequests = Invoke-RestMethod -Uri "$baseUrl/admin/card-block-requests" -Method GET -Headers $adminHeaders
    if ($blockRequests.Count -gt 0) {
        $requestId = $blockRequests[0].id
        $approveResult = Invoke-RestMethod -Uri "$baseUrl/admin/card-block-requests/$requestId/approve" -Method PUT -Headers $adminHeaders
        Write-Host "Block request $requestId approved successfully" -ForegroundColor Green
    } else {
        Write-Host "! No block requests to approve" -ForegroundColor Yellow
    }
} catch {
    Write-Host "Block request approval failed: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# Test 12: Error handling tests
Write-Host "12. Testing error handling..." -ForegroundColor Yellow

# Test invalid login
try {
    $invalidLogin = @{
        username = "nonexistent"
        password = "wrong"
    } | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method POST -Body $invalidLogin -ContentType "application/json"
    Write-Host "Invalid login should have failed" -ForegroundColor Red
} catch {
    Write-Host "Invalid login correctly rejected" -ForegroundColor Green
}

# Test unauthorized access
try {
    Invoke-RestMethod -Uri "$baseUrl/cards" -Method GET
    Write-Host "Unauthorized access should have failed" -ForegroundColor Red
} catch {
    Write-Host "Unauthorized access correctly rejected" -ForegroundColor Green
}

Write-Host ""
Write-Host "=== Test Summary ===" -ForegroundColor Green
Write-Host "All functionality tests completed. Check the results above for any failures." -ForegroundColor White
