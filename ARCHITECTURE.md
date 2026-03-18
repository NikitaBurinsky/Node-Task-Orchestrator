# ARCHITECTURE

## 1\. Назначение системы

**Node Task Orchestrator (NTO)** — это система для управления серверами и запуска shell-скриптов через HTTP API.

Основные сценарии:

* регистрация/аутентификация пользователя;
* управление серверами и группами серверов;
* хранение библиотеки скриптов;
* запуск скриптов на одном сервере, списке серверов или на группе;
* отслеживание статуса и вывода выполнения задач.

## 2\. Контекст и границы

```text
\\\\\\\[React SPA]
   |
   | HTTPS /api + JWT
   v
\\\\\\\[Nginx reverse proxy] -> \\\\\\\[Spring Boot backend]
                               |
                               +--> \\\\\\\[PostgreSQL]
                               |
                               +--> \\\\\\\[SSH servers via Apache Mina SSHD]
```

Внешние зависимости:

* PostgreSQL как основное хранилище;
* управляемые SSH-узлы (target servers);
* Nginx + Certbot для TLS-терминации и reverse proxy.

## 3\. Архитектурный стиль

Проект следует слоистой Clean Architecture (Onion-подход):

* `core` — доменная модель без привязки к фреймворкам.
* `application` — DTO, сервисные и репозиторные порты, контракты маппинга.
* `infrastructure` — реализации портов, JPA, security, SSH, cache, config, AOP.
* `web` — REST API (controllers + global exception advice).

Правило зависимостей:

* внешние слои зависят от внутренних;
* `core` не зависит от Spring/инфраструктуры;
* инфраструктура реализует интерфейсы уровня `application`.

## 4\. Backend-компоненты

### 4.1 Core (домен)

Сущности:

* `UserEntity`
* `ServerEntity`
* `ScriptEntity`
* `ServerGroupEntity`
* `TaskEntity`

Enum:

* `TaskStatus`: `PENDING`, `RUNNING`, `SUCCESS`, `FAILED`, `CANCELLED`.

Связи:

* `User 1:N ServerGroup` (владелец групп)
* `User 1:N Script`
* `Server M:N ServerGroup` (таблица `server\\\\\\\_groups`)
* `Server 1:N Task`
* `Script 1:N Task`
* `ServerGroup 1:N Task` через `Task.sourceGroup` (для массовых запусков)

Примечание: каждый пользователь получает дефолтную группу `Default` при регистрации,
а новые серверы автоматически добавляются в эту группу.

### 4.2 Application (контракты)

Порты сервисов:

* `AuthService`
* `ServerService`
* `ServerGroupService`
* `ScriptService`
* `TaskService`
* `ScriptExecutor` (стратегия выполнения)
* `MappingService`

Порты репозиториев:

* `UserRepository`, `ServerRepository`, `ScriptRepository`, `ServerGroupRepository`, `TaskRepository`.

DTO-модель:

* `ServerDto`, `ServerGroupDto`, `ScriptDto`, `TaskDto`, `UserDto`, `AuthRequestDto`, `AuthResponseDto`, `BulkTaskRequestDto`.

### 4.3 Infrastructure (реализации)

Сервисы:

* `AuthServiceImpl`
* `ServerServiceImpl`
* `ServerGroupServiceImpl`
* `ScriptServiceImpl`
* `TaskServiceImpl`
* `MappingServiceImpl`

Стратегии выполнения задач (`ScriptExecutor`):

* `MockScriptExecutor` (`nto.executor.type=mock`, default)
* `SshScriptExecutor` (`nto.executor.type=ssh`)

Асинхронность:

* `AsyncConfig` (`taskExecutor`: core=50, max=100, queue=1000)
* `executeAsync(...)` у executor-ов работает с `@Async("taskExecutor")` и `REQUIRES\\\\\\\_NEW`.

Кэш:

* `TaskStatusCache` хранит последние статусы:

  * по паре `(serverId, scriptId)`;
  * по `taskId`.

Security:

* `SecurityConfig` (stateless JWT)
* `JwtAuthenticationFilter` (парсинг `Authorization: Bearer ...`)
* `JwtUtils` (подпись/валидация токена)
* `CustomUserDetailsService`

Persistence:

* `Jpa\\\\\\\*Repository` на Spring Data JPA.
* Используются `@EntityGraph` для уменьшения N+1.
* `JpaTaskRepository` содержит JPQL/Native выборки для фильтрации задач.

Кросс-срезы:

* `ExecutionTimeAspect` + `@LogExecutionTime` для метрик времени выполнения методов.
* `OpenApiConfig` включает Swagger/OpenAPI + bearer security scheme.

### 4.4 Web (REST API)

Контроллеры:

* `/api/auth` — login/register
* `/api/servers` — CRUD + ping
* `/api/groups` — CRUD групп + управление составом + групповой ping/execute
* `/api/scripts` — CRUD скриптов
* `/api/tasks` — запуск задач, bulk, статус, история, поиск
* `/api/stats` — счётчики конкурентного выполнения

Ошибки:

* `GlobalExceptionHandler` обрабатывает validation errors и общий `Exception`.

## 5\. Frontend (React + TypeScript)

Стек:

* React 18, React Router, Axios, Tailwind, Vite.

Ключевые модули:

* `AuthContext` — хранение JWT в `localStorage`.
* `AuthGuard` — защита приватных маршрутов.
* `services/api.ts` — API-клиент и доменные фасады (`authApi`, `serversApi`, ...).

Маршруты:

* `/login`, `/register`
* `/dashboard`, `/servers`, `/groups`, `/groups/:id`, `/scripts`, `/tasks`, `/tasks/:id`

Модель взаимодействия:

* frontend прикладывает JWT в interceptor Axios;
* страницы `Dashboard`, `Tasks`, `TaskTerminal` используют polling для live-статусов.

## 6\. Основные потоки

### 6.1 Аутентификация

1. `POST /api/auth/register` или `POST /api/auth/login`.
2. При регистрации создаётся дефолтная группа пользователя (`Default`).
3. Backend возвращает JWT.
4. Frontend сохраняет токен в `localStorage`.
5. Все последующие запросы идут с `Authorization: Bearer <token>`.

### 6.2 Запуск задачи на сервере

1. Клиент вызывает `POST /api/tasks` (`serverId`, `scriptId`).
2. `TaskServiceImpl` валидирует доступ и создаёт `TaskEntity` со статусом `PENDING`.
3. Задача кладётся в `TaskStatusCache`.
4. Вызывается `ScriptExecutor.executeAsync(taskId)`.
5. Executor переводит статус в `RUNNING`, выполняет mock/ssh, сохраняет `output` и финальный статус.
6. Клиент опрашивает `GET /api/tasks/{id}` до завершения.

### 6.3 Групповой запуск

1. `POST /api/groups/{id}/execute?scriptId=...`.
2. Для каждого сервера группы создаётся отдельный `TaskEntity` с `sourceGroup`.
3. Все задачи асинхронно исполняются через `ScriptExecutor`.
4. Статус последнего группового запуска доступен через `GET /api/groups/{id}/status/last`.

## 7\. Модель данных (таблицы)

Ключевые таблицы:

* `users`
* `servers`
* `scripts`
* `groups`
* `server\\\\\\\_groups` (M:N server-group)
* `tasks`

`servers` не содержит прямого `user_id`: владение сервером определяется через группы (`groups.owner`).

`tasks` содержит operational-поля:

* `status`, `output`, `created\\\\\\\_at`, `started\\\\\\\_at`, `finished\\\\\\\_at`, `server\\\\\\\_id`, `script\\\\\\\_id`, `source\\\\\\\_group\\\\\\\_id`.

## 8\. Конфигурация и среды

Основные параметры (`application.properties`):

* `spring.datasource.\\\\\\\*`
* `spring.jpa.hibernate.ddl-auto=update`
* `nto.executor.type=ssh|mock`
* `nto.app.jwtSecret`
* `nto.app.jwtExpirationMs`

Важно:

* в Docker Compose backend запускается с `NTO\\\\\\\_EXECUTOR\\\\\\\_TYPE=ssh`.
* фронтенд ориентирован на `/api` (прод-домен `https://formatis.online/api`).

## 9\. Развертывание

Текущий контур (`docker-compose.yml`):

* `nto-backend` (Spring Boot jar)
* `nginx` (reverse proxy 80/443)
* `certbot` (автообновление сертификатов)

Поток трафика:

* клиент -> `nginx:443` -> `nto-backend:8080`.

## 10\. Нефункциональные аспекты

* Асинхронность выполнения задач через thread pool.
* Кэш последних статусов для ускорения чтений.
* Stateless security (JWT).
* OpenAPI-документация доступна через swagger endpoints.
* Логирование в файл `logs/nto-app.log`.

## 11\. Тестирование

Наличие unit-тестов:

* `TaskServiceImplTest`
* `ServerGroupServiceImplTest`
* `SshScriptExecutorTest`

Фокус покрытия: бизнес-правила и поведение сервисов при mocked-зависимостях.

## 12\. Точки расширения

* Новая стратегия выполнения: реализовать `ScriptExecutor` + `@ConditionalOnProperty`.
* Новые DTO/entity-мэппинги: добавить `MapperProfile<E,D>`.
* Новые источники данных: реализовать application-репозиторные порты.
* Масштабирование задач: вынести execution в очередь (RabbitMQ/Kafka) без изменения web-API.

## 13\. Ограничения текущего MVP

* `TaskStatusCache` in-memory, без распределённой синхронизации между инстансами.
* Нет RBAC/ролей (в `UserDetails` authority-пустые).
* Отсутствует единая модель доменных ошибок (часть ошибок возвращается как generic 500).
* Polling в UI вместо push-модели (WebSocket/SSE).
