# Chat Service

A Spring Boot microservice for **user–admin support communication**. It provides REST and WebSocket APIs for chat rooms, messages, and read-status tracking. Users (customers) have one support room each; admins can list and access all rooms.

---

## Table of Contents

- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Architecture](#architecture)
- [Configuration](#configuration)
- [Database Schema](#database-schema)
- [REST API Reference](#rest-api-reference)
- [WebSocket API](#websocket-api)
- [Authentication & Security](#authentication--security)
- [External Dependencies](#external-dependencies)
- [Running the Service](#running-the-service)

---

## Tech Stack

| Category        | Technology |
|----------------|------------|
| **Framework**  | Spring Boot 3.5.7 |
| **Java**       | 17 |
| **Database**   | PostgreSQL, Spring Data JPA |
| **Security**   | Spring Security, JWT (jjwt 0.11.5) |
| **Messaging** | Spring WebSocket (STOMP over SockJS) |
| **HTTP Client** | Spring Cloud OpenFeign |
| **Resilience** | Resilience4j Circuit Breaker |
| **Build**      | Maven |

---

## Project Structure

```
chat-service/
├── pom.xml
├── database_schema.sql          # Reference schema (JPA creates tables via ddl-auto)
├── README.md
├── src/main/
│   ├── java/com/expense/chat_service/
│   │   ├── ChatServiceApplication.java
│   │   ├── config/              # Security, CORS, WebSocket, Feign
│   │   │   ├── CorsConfig.java
│   │   │   ├── CustomAccessDeniedHandler.java
│   │   │   ├── CustomAuthenticationEntryPoint.java
│   │   │   ├── FeignConfig.java
│   │   │   ├── ObjectMapperConfig.java
│   │   │   ├── SecurityHeadersConfig.java
│   │   │   ├── WebSecurityConfig.java
│   │   │   └── WebSocketConfig.java
│   │   ├── controller/
│   │   │   └── ChatController.java
│   │   ├── dto/
│   │   │   ├── ChatMessageDto.java
│   │   │   ├── ChatRoomDto.java
│   │   │   ├── ExceptionResponse.java
│   │   │   └── SendMessageRequest.java
│   │   ├── entity/
│   │   │   ├── ChatMessage.java
│   │   │   ├── ChatRoom.java
│   │   │   └── ChatRoomRead.java
│   │   ├── fiegn/
│   │   │   └── UserClientService.java
│   │   ├── repository/
│   │   │   ├── ChatMessageRepository.java
│   │   │   ├── ChatRoomReadRepository.java
│   │   │   └── ChatRoomRepository.java
│   │   ├── request/
│   │   │   └── Principle.java
│   │   ├── response/
│   │   │   ├── BaseResponse.java
│   │   │   ├── StatusResponse.java
│   │   │   └── UserDataResponse.java
│   │   ├── security/
│   │   │   ├── JwtRequestFilter.java
│   │   │   ├── TokenGenerator.java
│   │   │   ├── TokenValidation.java
│   │   │   └── ValidationResponse.java
│   │   ├── service/
│   │   │   ├── ChatService.java
│   │   │   ├── UserService.java
│   │   │   └── impl/
│   │   │       └── ChatServiceImpl.java
│   │   └── utils/
│   │       └── CommonUtil.java
│   └── resources/
│       └── application.yml
└── target/
```

---

## Architecture

- **Layered**: Controller → Service → Repository; DTOs for API, entities for persistence.
- **REST + WebSocket**: Chat can be used via REST or STOMP over SockJS; sending a message via WebSocket broadcasts to `/topic/room/{roomId}`.
- **Security**: Stateless JWT; `JwtRequestFilter` validates `Authorization: Bearer <token>` for REST; WebSocket CONNECT uses the same token (header `Authorization` or `token`).
- **User data**: Room display names come from **user-service** via OpenFeign (`UserClientService`). Feign forwards the incoming request’s `Authorization` header.
- **Roles**: `USER` — own room only; `ADMIN` — all rooms. Enforced in `ChatServiceImpl` (e.g. `canAccessRoom`, `isAdmin`).

---

## Configuration

| Property | Env Variable | Default | Description |
|----------|--------------|---------|-------------|
| Server port | — | `8087` | HTTP port |
| Context path | — | `/chat-service/api` | All REST and WS paths are under this prefix |
| DB URL | `DB_URL` | `jdbc:postgresql://localhost:5432/expense_tracker` | PostgreSQL JDBC URL |
| DB user | `DB_USERNAME` | `postgres` | Database username |
| DB password | `DB_PASSWORD` | `postgres` | Database password |
| JWT secret | `JWT_SECRET` | (dev default in yml) | Signing key for JWT |
| JWT expiration | `JWT_EXPIRATION` | `86400` | Token TTL in seconds |
| JWT audience | `JWT_AUDIENCE` | (UUID in yml) | JWT audience |
| User service URL | `USER_SERVICE_URL` | `http://localhost:8019/user-service/api` | Base URL for Feign user-service client |
| CORS origins | `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Comma-separated origins for CORS |

JPA: `ddl-auto: update` (schema created/updated by Hibernate). See `database_schema.sql` for reference DDL.

---

## Database Schema

Schema name: **`conf`**. Main tables:

| Table | Purpose |
|-------|---------|
| `conf.chat_room` | One support room per user (`user_id` unique). |
| `conf.chat_message` | Messages in a room; `sender_id`, `sender_role` (USER/ADMIN), `content`, `created_at`. |
| `conf.chat_room_read` | Per user per room: `last_read_at` for unread count. Unique on `(chat_room_id, user_id)`. |

See `database_schema.sql` for full column and index definitions.

---

## REST API Reference

**Base URL:** `http://<host>:8087/chat-service/api/chat`

All endpoints require **JWT** in header: `Authorization: Bearer <token>`.

---

### 1. Get chat rooms

**`GET /chat/rooms`**

- **Auth:** Required (USER or ADMIN).
- **Behavior:**  
  - **USER:** Returns list containing their single support room (if any).  
  - **ADMIN:** Returns all rooms ordered by `updatedAt` descending.
- **Response:** `200 OK` — JSON array of `ChatRoomDto`.

**ChatRoomDto:**

```json
{
  "id": 1,
  "userId": 10,
  "userDisplayName": "John Doe",
  "userEmail": null,
  "createdAt": "2025-01-15T10:00:00",
  "updatedAt": "2025-01-20T14:30:00",
  "messages": null,
  "unreadCount": 2
}
```

| Field | Type | Description |
|-------|------|-------------|
| `id` | number | Room ID |
| `userId` | number | Owner (customer) user ID |
| `userDisplayName` | string | From user-service (e.g. first + last name) |
| `userEmail` | string | Optional |
| `createdAt` | string (ISO-8601) | Room creation time |
| `updatedAt` | string (ISO-8601) | Last activity |
| `messages` | array or null | Populated only for get room by id |
| `unreadCount` | number | Messages from other party after current user’s last read |

---

### 2. Get room by ID

**`GET /chat/rooms/{roomId}`**

- **Auth:** Required. USER may access only their room; ADMIN may access any.
- **Path:** `roomId` — numeric room ID.
- **Response:** `200 OK` — single `ChatRoomDto` with `messages` populated.
- **Errors:** `403` if no access; `404` if room not found (mapped from `IllegalArgumentException`).

---

### 3. Create support room

**`POST /chat/rooms`**

- **Auth:** Required. **USER only** (admins get error).
- **Body:** None.
- **Behavior:** If the user already has a room, returns that room; otherwise creates one (one room per user).
- **Response:** `200 OK` — `ChatRoomDto` (created or existing).
- **Errors:** `400` if caller is ADMIN (“Admins cannot create a support room”).

---

### 4. Get messages (paginated)

**`GET /chat/rooms/{roomId}/messages?page=0&size=50`**

- **Auth:** Required. Same access rules as get room (USER own room, ADMIN any).
- **Path:** `roomId` — numeric room ID.
- **Query:**  
  - `page` — zero-based page (default `0`).  
  - `size` — page size (default `50`).
- **Response:** `200 OK` — JSON array of `ChatMessageDto`.
- **Errors:** `403` no access; `404` room not found.

**ChatMessageDto:**

```json
{
  "id": 1,
  "chatRoomId": 1,
  "senderId": 10,
  "senderRole": "USER",
  "senderName": "John Doe",
  "content": "Hello, I need help.",
  "createdAt": "2025-01-15T10:05:00"
}
```

| Field | Type | Description |
|-------|------|-------------|
| `id` | number | Message ID |
| `chatRoomId` | number | Room ID |
| `senderId` | number | Sender user ID |
| `senderRole` | string | `USER` or `ADMIN` |
| `senderName` | string | Display name |
| `content` | string | Message body |
| `createdAt` | string (ISO-8601) | Send time |

---

### 5. Send message (REST)

**`POST /chat/rooms/{roomId}/messages`**

- **Auth:** Required. Same room access rules.
- **Path:** `roomId` — numeric room ID.
- **Request body:** JSON object with key `content` (required, non-blank).

**Request body example:**

```json
{
  "content": "Hello, I need help with my account."
}
```

- **Response:** `200 OK` — `ChatMessageDto` of the saved message. Same message is broadcast to WebSocket subscribers of `/topic/room/{roomId}`.
- **Errors:** `400` if `content` missing or blank; `403` no access; `404` room not found.

---

### 6. Mark room as read

**`POST /chat/rooms/{roomId}/read`**

- **Auth:** Required. Same room access rules.
- **Path:** `roomId` — numeric room ID.
- **Body:** None.
- **Behavior:** Sets or updates `last_read_at` for the current user in this room (used for unread count).
- **Response:** `200 OK` — no body.
- **Errors:** `403` no access; `404` room not found.

---

## WebSocket API

- **Endpoint (with SockJS):** `http://<host>:8087/chat-service/api/ws`
- **Protocol:** STOMP over SockJS.
- **Auth:** On CONNECT, send JWT in header `Authorization: Bearer <token>` or `token: <token>`.

**Broker prefixes:**

- **Subscribe (receive):** `/topic/room/{roomId}` — new messages in that room.
- **Send (application):** `/app/chat.send` — send a message.

**Send message via WebSocket**

- **Destination:** `/app/chat.send`
- **Payload (JSON):** `SendMessageRequest`

```json
{
  "chatRoomId": 1,
  "content": "Hello from WebSocket."
}
```

- **Validation:** `chatRoomId` required; `content` required and not blank.
- **Server response:** Same as REST — the saved `ChatMessageDto` is returned on the same request-reply (if used). The same DTO is broadcast to all subscribers of `/topic/room/{roomId}`.

**Allowed origins (WebSocket):** `http://localhost:5173`, `http://localhost:3000` (see `WebSocketConfig`).

---

## Authentication & Security

- **REST:** Every request (except `/actuator/**` and `/ws/**`) must include `Authorization: Bearer <jwt>`. `JwtRequestFilter` validates the token and sets `SecurityContext`; invalid/missing token returns `401` with JSON `{ "message": "..." }`.
- **WebSocket:** Token is validated on CONNECT; principal is attached to the session and used for `/app/chat.send`.
- **JWT:** Signed with configured secret; payload includes a `user` claim that is mapped to `Principle` (id, firstName, lastName, email, userType, etc.). `userType` is `USER` or `ADMIN` and drives access control.
- **CORS:** Configured in `WebSecurityConfig` (and `CorsConfig`) with configurable origins, methods, and headers.
- **Headers:** `SecurityHeadersConfig` adds X-Frame-Options, X-Content-Type-Options, CSP, etc.

---

## External Dependencies

**User Service (OpenFeign)**

- **Base URL:** `user.service.url` (e.g. `http://localhost:8019/user-service/api`).
- **Used for:** Resolving room owner display name (e.g. `GET /user/by-id/{id}`). Feign uses the current request’s `Authorization` header.
- **Health:** Feign can call `GET /actuator/health` on user-service; circuit breaker fallbacks are defined in `UserService` for failures.

---

## Running the Service

**Prerequisites:** Java 17+, Maven, PostgreSQL (schema can be created by JPA), and optionally user-service for full behavior.

1. **Database:** Ensure PostgreSQL is running and `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` point to it (or use defaults in `application.yml`).
2. **Optional:** Start user-service and set `USER_SERVICE_URL` if you use room listing with display names.
3. **Build and run:**

```bash
mvn clean install
mvn spring-boot:run
```

4. **Base URLs:**
   - REST: `http://localhost:8087/chat-service/api/chat`
   - WebSocket: `http://localhost:8087/chat-service/api/ws` (with SockJS)

**Environment variables (summary):** `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `JWT_EXPIRATION`, `JWT_AUDIENCE`, `USER_SERVICE_URL`, `CORS_ALLOWED_ORIGINS`.

---

## Error Handling

- **401 Unauthorized:** Missing or invalid JWT (REST or WebSocket CONNECT). Body: `{ "message": "..." }`.
- **403 Forbidden:** Valid JWT but no permission (e.g. USER accessing another user’s room). Handled by `CustomAccessDeniedHandler`.
- **404:** Room not found (e.g. `IllegalArgumentException("Chat room not found: ...")` mapped by Spring).
- **400:** Validation (e.g. send message without `content`). No global `@ControllerAdvice`; some endpoints return `ResponseEntity.badRequest().build()`.

---

## Summary

| Area | Details |
|------|---------|
| **Purpose** | User–admin support chat: one room per user, REST + WebSocket. |
| **Roles** | USER (own room), ADMIN (all rooms). |
| **Persistence** | PostgreSQL, schema `conf`, JPA `ddl-auto: update`. |
| **APIs** | REST under `/chat-service/api/chat`, WebSocket at `/chat-service/api/ws`. |
| **Auth** | JWT in `Authorization` header (and `token` for WebSocket CONNECT). |
| **Integration** | OpenFeign user-service for display names and optional health check. |
