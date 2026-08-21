
#  Temfex

[![Java](https://img.shields.io/badge/Java-25-orange?style=for-the-badge&logo=openjdk)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4-brightgreen?style=for-the-badge&logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?style=for-the-badge&logo=postgresql)](https://www.postgresql.org/)
[![MinIO](https://img.shields.io/badge/MinIO-S3_Compatible-red?style=for-the-badge&logo=minio)](https://min.io/)
[![CI](https://github.com/Coociks/Temfex/actions/workflows/ci.yml/badge.svg)](https://github.com/Coociks/Temfex/actions/workflows/ci.yml)

Сервис для безопасного и временного обмена файлами с ограниченным сроком жизни, лимитом скачиваний и защитой паролем (аналог WeTransfer).

##  Особенности
-  **Гибкие настройки ссылок**: срок жизни (TTL), лимит скачиваний, защита паролем.
-  **Множественная загрузка**: автоматическая ZIP-архивация нескольких файлов "на лету" (в памяти) без сохранения на диск сервера.
-  **Rate Limiting**: защита API от спама и DDoS с помощью Bucket4j (разные лимиты для загрузки и скачивания).
-  **Предпросмотр**: встроенный просмотр изображений и PDF прямо в браузере.
-  **QR-коды**: мгновенная генерация QR-кода для удобной передачи ссылки на мобильные устройства.
-  **Автоочистка**: фоновая задача (`@Scheduled`) автоматически удаляет просроченные файлы из БД и хранилища.
-  **Статистика**: отслеживание количества скачиваний и последних запросов.
-  **Документация**: полная OpenAPI/Swagger документация.

##  Стек технологий
- **Backend**: Java 25, Spring Boot 3.4, Spring Security, Spring Data JPA
- **Database**: PostgreSQL 16, Flyway (миграции)
- **Storage**: MinIO (S3 API)
- **Rate Limiting**: Bucket4j
- **Documentation**: Springdoc OpenAPI (Swagger)
- **DevOps**: Docker, Docker Compose, GitHub Actions

##  Архитектура
` + "```mermaid" + `
graph TD
    Client[Клиент / Браузер] -->|REST API| API[Spring Boot API]
    API -->|JPA/Hibernate| DB[(PostgreSQL)]
    API -->|S3 API| Storage[(MinIO)]
    Scheduler[Фоновая задача @Scheduled] -->|Удаление| DB
    Scheduler -->|Удаление| Storage
` + "```" + `

##  Быстрый старт

### Требования
- Java 25
- Docker & Docker Compose

### Запуск
1. Клонируйте репозиторий:
   ` + "```bash" + `
   git clone https://github.com/Coociks/Temfex.git
   cd Temfex
   ` + "```" + `
2. Запустите инфраструктуру (PostgreSQL + MinIO):
   ` + "```bash" + `
   docker-compose up -d
   ` + "```" + `
3. Запустите приложение:
   ` + "```bash" + `
   ./mvnw spring-boot:run
   ` + "```" + `
4. Откройте в браузере:
   - Приложение: http://localhost:8080
   - Swagger UI: http://localhost:8080/swagger-ui.html

##  API Документация
Полная документация всех эндпоинтов с примерами запросов доступна через Swagger UI после запуска приложения.
"@ | Set-Content -Path "README.md" -Encoding UTF8