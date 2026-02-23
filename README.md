# NTO (Node Task Orchestrator) 🚀

**NTO** — это MVP-система для удаленного управления серверами и выполнения скриптов через SSH. Проект разработан как учебная база для освоения экосистемы Spring Boot (Java 21).

## 🛠 Технологический стек

- **Backend:** Java 21, Spring Boot
- **Proxy:** Nginx 
- **Data:** Spring Data JPA, PostgreSQL, Hibernate (L2 Cache & EntityGraph)
- **Concurrency:** CompletableFuture, `@Async`, ThreadPoolTaskExecutor, Atomic Types
- **Security:** Spring Security + JWT 
- **Architecture:** Clean Architecture (Core/Application/Infrastructure/Web)
- **Documentation:** Springdoc-OpenAPI (Swagger)

## 🏗 Архитектура (Clean Architecture)

Проект разделен на слои, что минимизирует связность (аналог Onion/Clean в .NET):

1.  **Core:** "Plain Old Java Objects" (Entities, Enums). Не имеет зависимостей от фреймворков.
2.  **Application:** Интерфейсы (Input/Output ports), DTO (Records), бизнес-логика и `MappingService`.
3.  **Infrastructure:** Реализация репозиториев, SSH-клиента, кэширования и конфигурации бинов.
4.  **Web:** REST-контроллеры и `@ControllerAdvice` (аналог Middleware для обработки исключений).

## 🧩 Ключевые особенности

### Custom Mapping System
Вместо MapStruct используется самописная система `MappingService`. 
- Регистрация мапперов через профили `MapperProfile<E, D>`.
- Поддержка разрешения зависимостей (например, поиск Entity по ID при маппинге из DTO).

### Execution Strategy (Pattern Strategy)
Система поддерживает два режима исполнения задач (переключается в `application.properties` через `nto.executor.type`):
- `mock`: Имитация задержки (для разработки).
- `ssh`: Реальное подключение к серверам через JSch.

### Concurrency & Performance
- **Async Workers:** Задачи выполняются в кастомном `ThreadPoolTaskExecutor` (префикс `NtoWorker-`).
- **N+1 Solution:** Использование `@EntityGraph` для жадной загрузки связанных коллекций (Server -> Groups).
- **L1 Cache:** In-memory кэш статусов задач на базе `ConcurrentHashMap` для быстрого поллинга.

## 🚀 Быстрый старт

1. **База данных:** Поднимите PostgreSQL и создайте БД `nto_db`.
2. **Конфигурация:** Настройте `src/main/resources/application.properties`:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/nto_db
   nto.executor.type=mock # или ssh для боевого режима
