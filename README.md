# NTO (Node Task Orchestrator) 🚀

**NTO** — это MVP-система для удаленного управления серверами и выполнения скриптов через SSH. Проект разработан как учебная база для освоения экосистемы Spring Boot (Java 21).

## 🛠 Технологический стек

- **Backend:** Java 21, Spring Boot
- **Proxy:** Nginx 
- **Data:** Spring Data JPA, PostgreSQL, Hibernate (Cache & EntityGraph)
- **Concurrency:** CompletableFuture, `@Async`, SshScriptExecutor, Atomic Types
- **Security:** Spring Security + JWT 
- **Architecture:** Clean Architecture (Core/Application/Infrastructure/Web)
- **Documentation:** Springdoc-OpenAPI (Swagger)

## 🏗 Архитектура (Clean Architecture)

Проект разделен на слои, что минимизирует связность (Минимизированная Onion Architecture):

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

## Ownership Model

- Users own server groups.
- Each user gets a default group (`Default`) on registration.
- Servers do not reference users directly; new servers are added to the user's default group.
