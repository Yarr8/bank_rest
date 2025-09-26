## Инструкция запуска
### Разработка (dev)
```bash
# Запуск в режиме разработки
docker-compose -f docker-compose.yml up --build

# Или с переменными окружения из файла
docker-compose --env-file env.dev -f docker-compose.yml up --build
```

**Особенности dev-режима:**
- Автоматически создается тестовый админ: `admin` / `admin123`
- Подключение к БД: `localhost:5432`
- Приложение доступно на: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- CORS настроен для: `http://localhost:3000,http://localhost:8080`

### Продакшн (prod)
```bash
# 1. Настройте переменные окружения
cp env.prod .env
# Отредактируйте .env файл с реальными значениями
# ОБЯЗАТЕЛЬНО укажите CORS_ALLOWED_ORIGINS с вашими доменами!

# 2. Запуск в продакшене
docker-compose -f docker-compose.prod.yml up --build -d
```

### Переменные окружения

**Database connection:**
- `DB_HOST` - Хост базы данных (например: `postgres`)
- `DB_PORT` - Порт базы данных (например: `5432`)
- `DB_NAME` - Имя базы данных (например: `bankcards`)
- `DB_USERNAME` - Пользователь БД (например: `user`)
- `DB_PASSWORD` - Пароль БД (например: `StrongPassword123!`)

**JWT configuration:**
- `JWT_SECRET` - Секрет для JWT (минимум 32 символа) (например: `mySecretKey123456789012345678901234567890`)
- `JWT_EXPIRATION` - Время жизни токена в миллисекундах (например: `86400000`)

**Encryption:**
- `CARD_ENCRYPTION_KEY` - Ключ шифрования карт (16 символов) (например: `MySecretKey12345`)

**Application:**
- `SERVER_PORT` - Порт приложения (например: `8080`)

**CORS Configuration:**
- `CORS_ALLOWED_ORIGINS` - Разрешенные домены для CORS (например: `http://localhost:3000,https://yourdomain.com`)
- `CORS_ALLOWED_METHODS` - Разрешенные HTTP методы (например: `GET,POST,PUT,DELETE,OPTIONS`)
- `CORS_ALLOWED_HEADERS` - Разрешенные заголовки (например: `*` или `Content-Type,Authorization`)
- `CORS_ALLOW_CREDENTIALS` - Разрешить отправку cookies и токенов (например: `true`)
- `CORS_MAX_AGE` - Время кеширования preflight запросов в секундах (например: `3600`)

### Примеры конфигурации CORS

**Для разработки:**
```bash
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:8080
CORS_ALLOWED_METHODS=GET,POST,PUT,DELETE,OPTIONS
CORS_ALLOWED_HEADERS=*
CORS_ALLOW_CREDENTIALS=true
CORS_MAX_AGE=3600
```

**Для продакшена:**
```bash
CORS_ALLOWED_ORIGINS=https://yourdomain.com,https://www.yourdomain.com
CORS_ALLOWED_METHODS=GET,POST,PUT,DELETE,OPTIONS
CORS_ALLOWED_HEADERS=*
CORS_ALLOW_CREDENTIALS=true
CORS_MAX_AGE=3600
```
