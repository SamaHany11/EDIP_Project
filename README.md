<p align="center">
  <h1 align="center">EDIP — Electronic Document Intelligence Platform</h1>
  <p align="center">
    A production-grade <strong>Spring Boot backend</strong> for intelligent enterprise document management,<br/>
    featuring AI-powered OCR, auto-classification, smart routing, and a RAG-based document chatbot.
  </p>
  <p align="center">
    <img src="https://img.shields.io/badge/Java-17+-orange?style=flat-square&logo=java"/>
    <img src="https://img.shields.io/badge/Spring%20Boot-3.2.8-brightgreen?style=flat-square&logo=springboot"/>
    <img src="https://img.shields.io/badge/Database-SQL%20Server%20%2B%20MongoDB-blue?style=flat-square"/>
    <img src="https://img.shields.io/badge/Security-JWT%20%2B%20Spring%20Security-red?style=flat-square"/>
    <img src="https://img.shields.io/badge/AI-OCR%20%2B%20RAG%20Chatbot-purple?style=flat-square"/>
    <img src="https://img.shields.io/badge/Deployment-Railway-black?style=flat-square&logo=railway"/>
    <img src="https://img.shields.io/badge/Container-Docker-2496ED?style=flat-square&logo=docker"/>
    <img src="https://img.shields.io/badge/API%20Docs-Swagger%20UI-85EA2D?style=flat-square&logo=swagger"/>
    <img src="https://img.shields.io/badge/NoSQL-MongoDB-47A248?style=flat-square&logo=mongodb"/>
  </p>
</p>

---

## What is EDIP?

EDIP is an enterprise backend system that replaces paper-based document workflows with a fully digital, AI-assisted pipeline. When a user submits a document, the system automatically extracts its text via OCR, classifies its type, routes it to the correct department, tracks every action taken on it, and allows stakeholders to interact with its content through a conversational chatbot — all without manual intervention.

---

## Features

- AI-powered OCR text extraction from uploaded documents
- Intelligent document classification (type + sub-type detection)
- Automated department routing based on AI classification results
- RAG-based document chatbot (Groq LLM + HuggingFace)
- JWT authentication with server-side session invalidation
- Encrypted file storage via MongoDB GridFS
- Role-based access control (Admin / Head / Employee / External)
- Real-time role-specific dashboards with aggregated stats
- Full audit logging on every document action
- In-app notification system
- Strategy-pattern search with role-aware filtering
- Pre-built document templates (Leave, Transfer, Resignation)
- Dockerized and deployed on Railway

---

## Backend Architecture

```
src/main/java/com/example/EDIP/
|
+-- Auth/                   -> Registration, Login, Email Verification, JWT, Session
+-- account/                -> Employee account CRUD (Admin-managed)
|
+-- document/               -> Core document lifecycle engine
|   +-- ai/                 -> AIProcessingClient (async OCR + classification via HuggingFace)
|   +-- routing/            -> RoutingRulesService (AI result -> target department)
|   +-- model/
|   |   +-- sql/            -> Document, Attachment, Tracking, Reply, Notification, AutoLog
|   |   +-- mongo/          -> OcrDocument, ClassificationDocument (GridFS)
|   +-- service/            -> DocumentService, HeadService, EmployeeService,
|   |                          TrackingService, NotificationService, AutoLogService
|   +-- security/           -> CryptoService (file encryption)
|
+-- chatbot/                -> RAG chatbot (sessions, messages, RagClient -> Groq LLM)
+-- search/                 -> Strategy-pattern search (role-aware: Admin/Head/Employee/External)
+-- Dashboard/              -> Role-specific dashboard aggregates
+-- template/               -> Pre-built document templates (Leave, Transfer, Resignation)
+-- feedback/               -> Feedback submission & management
+-- settings/               -> Profile, password, organization settings
```

---

## Core Document Workflow

This is the heart of the system. Every document goes through a structured lifecycle:

```
[User Submits Document]
        |
        v
  File saved to MongoDB GridFS
  Document record created in SQL Server (status: SUBMITTED)
        |
        v  (Async, non-blocking)
  +------------------------------------------+
  |         AI Processing Pipeline           |
  |                                          |
  |  1. Fetch file bytes from MongoDB        |
  |  2. POST to HuggingFace OCR API          |
  |     -> Extracts raw text                 |
  |     -> Saves OcrDocument to MongoDB      |
  |  3. POST to Classification API           |
  |     -> Returns: documentType + subType   |
  |     -> department name (from AI output)  |
  |     -> Saves ClassificationDocument      |
  |  4. RoutingRulesService resolves         |
  |     documentType -> target department ID |
  |  5. Document routed + status -> PENDING  |
  |  6. RAG processing triggered (async)     |
  |     -> Document indexed for chatbot      |
  +------------------------------------------+
        |
        v
  [Department Head receives document]
        |
        +--- Assign to Employee
        |         |
        |         +-> Employee creates Reply (text + file)
        |                   |
        |                   +-> Head reviews Reply
        |                          +- APPROVE  -> Reply sent to submitter, status -> COMPLETED
        |                          +- RETURN   -> Back to employee for revision
        |                          +- REJECT   -> Rejected with reason
        |
        +--- Forward to Another Department
        |         +-> Restarts workflow in new department
        |
        +--- Complete Document directly
                  +-> Attach final signed files, status -> COMPLETED
```

### Document Status States

| Status | Meaning |
|---|---|
| `SUBMITTED` | Just received, awaiting AI processing |
| `PENDING` | AI processed, routed, awaiting action |
| `APPROVED` | Reply approved by Head |
| `COMPLETED` | Fully resolved and closed |

---

## AI Integration Details

### OCR + Classification (Async with Retry)

The `AIProcessingClient` processes every submitted document asynchronously using `@Async` + `@Retryable` (3 attempts, 2s backoff):

```
POST {AI_API_URL}
  Header: X-Groq-Api-Key
  Body:   multipart file (fetched from MongoDB GridFS)

Response:
  {
    "ocr_text": "...",
    "document_type": "leave_request",
    "sub_type": "annual_leave",
    "department": "HR"
  }
```

- OCR text is stored as `OcrDocument` in MongoDB
- Classification result is stored as `ClassificationDocument` in MongoDB
- `RoutingRulesService` normalizes the department name and resolves it to a department UUID
- If the AI returns a direct department name, it bypasses routing rules for direct assignment

### RAG Chatbot

Every document gets indexed in the RAG system (via HuggingFace + Groq LLM) once AI processing completes. Users can then:

1. Start a chat session on any document they have access to
2. Ask questions about the document's content
3. Switch documents within the same session
4. The LLM answers are grounded in the actual document text — not hallucinated

```
User -> POST /api/chatbot/ask
         +-> ChatbotService validates session + access
         +-> RagClient sends (question + session context) to RAG API
         +-> Response saved as ChatMessage
         +-> Answer returned to user
```

---

## Security Architecture

### JWT + Session Management

- On login, a JWT is issued and its **hash** is stored in `UserSession` (SQL)
- Every request passes through `JWTFilter` which validates the token and checks the stored hash
- This enables **server-side session invalidation** (logout actually works)
- `TokenHashUtil` handles the SHA-256 hashing before DB storage

### Role-Based Access Control

All endpoints are secured with `@PreAuthorize`. The four roles and their permissions:

| Role | Core Permissions |
|---|---|
| `EXTERNAL` | Submit documents, track own documents, view approved replies, use chatbot |
| `EMPLOYEE` | All of EXTERNAL + view department docs, create replies, forward to colleagues |
| `HEAD` | All of EMPLOYEE + assign, forward to departments, approve/reject/return replies, complete documents, access confidential docs |
| `ADMIN` | Full access — account management, all documents, all dashboards, audit logs |

### File Security

- All files are stored encrypted in MongoDB GridFS via `CryptoService`
- Confidential documents are only accessible to `HEAD` and `ADMIN`
- File downloads go through access-validation before serving bytes

---

## Search System — Strategy Pattern

The search layer uses the **Strategy design pattern** to return role-appropriate results:

```java
// SearchStrategyFactory resolves the correct strategy by role
SearchStrategy strategy = strategyFactory.get(currentUser);
SearchResponse result = strategy.search(criteria);
```

| Strategy | Scope |
|---|---|
| `AdminSearchStrategy` | All documents + all users across the system |
| `HeadSearchStrategy` | Department-scoped documents + users |
| `EmployeeSearchStrategy` | Assigned documents only |
| `ExternalSearchStrategy` | Own submitted documents only |

---

## Dashboard Data

Three role-specific dashboard endpoints aggregate real-time stats:

- **Admin Dashboard** — total documents, completion rate, top-performing departments, status breakdown
- **Head Dashboard** — department-level stats, pending queue, employee workload
- **Employee Dashboard** — personal stats, assigned documents count, completion rate

All dashboard queries use SQL projections (`DeptDocStatsProjection`, `EmployeeStatsProjection`, etc.) for optimized, minimal-fetch queries.

---

## API Reference

Base URL: `https://edipbackend-production.up.railway.app`
Swagger UI: `https://edipbackend-production.up.railway.app/swagger-ui/index.html`

### Auth

| Method | Endpoint | Role | Description |
|---|---|---|---|
| POST | `/api/auth/register` | Public | Register new user |
| POST | `/api/auth/login` | Public | Login, receive JWT |
| POST | `/api/auth/verify-email` | Public | Email OTP verification |
| POST | `/api/auth/forgot-password` | Public | Send reset email |
| POST | `/api/auth/reset-password` | Public | Reset with token |
| POST | `/api/auth/logout` | Authenticated | Invalidate session |

### Documents

| Method | Endpoint | Role | Description |
|---|---|---|---|
| POST | `/api/documents/submit` | All | Submit document (multipart) |
| GET | `/api/documents/my-documents` | All | Paginated own documents |
| GET | `/api/documents/department-documents` | EMPLOYEE, HEAD | Department document queue |
| GET | `/api/documents/{id}/tracking` | EXTERNAL | Full tracking history |
| GET | `/api/documents/{id}/summary` | EMPLOYEE+ | AI-generated summary |
| GET | `/api/documents/word-preview` | All | Convert doc to HTML preview |
| PUT | `/api/documents/{id}/assign-to-employee` | HEAD | Assign to employee |
| PUT | `/api/documents/{id}/forward-to-department` | HEAD | Forward with signed file |
| POST | `/api/documents/{id}/create-reply` | EMPLOYEE, HEAD | Draft a reply |
| PUT | `/api/documents/replies/{id}/approve` | HEAD | Approve reply |
| PUT | `/api/documents/replies/{id}/reject` | HEAD | Reject with reason |
| PUT | `/api/documents/replies/{id}/return-to-employee` | HEAD | Send back for revision |
| PUT | `/api/documents/{id}/complete` | HEAD | Complete with final files |
| GET | `/api/documents/confidential` | HEAD, ADMIN | Confidential documents only |
| GET | `/api/documents/audit-logs` | EMPLOYEE+ | System audit trail |

### Chatbot

| Method | Endpoint | Role | Description |
|---|---|---|---|
| POST | `/api/chatbot/ask` | Authenticated | Ask question (new or existing session) |
| GET | `/api/chatbot/sessions` | Authenticated | List all chat sessions |
| GET | `/api/chatbot/sessions/{id}/messages` | Authenticated | Session message history |

### Search

| Method | Endpoint | Role | Description |
|---|---|---|---|
| POST | `/api/search/documents` | Authenticated | Role-aware document search |
| POST | `/api/search/users` | HEAD, ADMIN | Search users |

---

## Deployment

The backend is deployed and live on **Railway**:

> **`https://edipbackend-production.up.railway.app`**

Railway automatically builds from the `Dockerfile` and injects all environment variables via its dashboard. The deployed instance connects to:

- **SQL Server** hosted on a remote Azure-compatible SQL instance
- **MongoDB Atlas** for OCR results, file storage, and RAG indexing
- **HuggingFace Spaces** for the AI processing and RAG APIs

### Deploy Your Own Instance on Railway

1. Push the repo to GitHub
2. Go to [railway.app](https://railway.app) -> **New Project** -> **Deploy from GitHub repo**
3. Select your repository
4. Railway auto-detects the `Dockerfile` and builds it
5. Go to **Variables** tab and add all keys from `.env.example`
6. Railway assigns a public URL automatically — set it as `FRONTEND_URL` in your frontend

---

## Getting Started (Local)

### Prerequisites

- Java 17+
- Maven 3.8+
- SQL Server instance
- MongoDB Atlas cluster

### 1. Clone

```bash
git clone https://github.com/YOUR_USERNAME/EDIP-Backend.git
cd EDIP-Backend
```

### 2. Configure Environment

```bash
cp .env.example .env
# Fill in your credentials — never commit the .env file
```

```env
DB_URL=jdbc:sqlserver://<host>:<port>;databaseName=<db>
DB_USERNAME=your_username
DB_PASSWORD=your_password

MONGO_URI=mongodb+srv://<user>:<pass>@<cluster>.mongodb.net/

JWT_SECRET=your_base64_secret

MAIL_USERNAME=your@email.com
MAIL_PASSWORD=your_app_password
BREVO_API_KEY=your_brevo_key

FRONTEND_URL=http://localhost:3000

AI_API_URL=https://your-space.hf.space/api/v1/process
RAG_BASE_URL=https://your-rag-space.hf.space
AI_API_KEY=your_groq_api_key
RAG_GROQ_KEY=your_rag_groq_key
```

### 3. Run

```bash
./mvnw spring-boot:run
```

### 4. Docker

```bash
docker build -t edip-backend .
docker run -p 8080:8080 --env-file .env edip-backend
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.2.8 |
| Language | Java 17 |
| SQL Database | Microsoft SQL Server |
| NoSQL / File Storage | MongoDB Atlas + GridFS |
| Security | Spring Security + JWT (jjwt 0.11.5) |
| AI Communication | Spring WebFlux (WebClient, reactive) |
| Retry Logic | Spring Retry (`@Retryable`) |
| Email | Spring Mail + Brevo API |
| API Docs | SpringDoc OpenAPI 2.5 (Swagger UI) |
| Build | Maven |
| Container | Docker |
| Deployment | Railway |

---

## Team
| Name               | Role                                |
|--------------------|-------------------------------------|
| Sama Hany          | Backend Developer                   |
| Shahd Rabea        | Backend Developer & DevOps Engineer |
| Manar Saber        | AI Engineer                         |
| Esraa Ahmed        | Frontend Developer                 |
| Rana Mohamed       | Testing                             |
| Mennatallah Ahmed  | Testing                             |

---

## License

This project was developed as an academic graduation project — All Rights Reserved.
