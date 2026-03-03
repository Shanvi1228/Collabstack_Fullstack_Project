# 📝 CollabDocs — Real-Time Collaborative Editor with AI

> A full-stack Google Docs-inspired collaborative editor with live multi-user editing,
> rich text formatting, and an AI chatbot powered by RAG — using your document as context.

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.3-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![React](https://img.shields.io/badge/React_18-20232A?style=for-the-badge&logo=react&logoColor=61DAFB)
![TypeScript](https://img.shields.io/badge/TypeScript_5-007ACC?style=for-the-badge&logo=typescript&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)
![OpenAI](https://img.shields.io/badge/OpenAI-412991?style=for-the-badge&logo=openai&logoColor=white)

---

## 📸 Demo


<!-- Once you have a demo GIF, replace this comment with:
![Demo](./demo/demo.gif)
-->

---

## ✨ Features

- 🔄 **Real-Time Collaboration** — Multiple users edit the same document simultaneously via WebSockets (STOMP over SockJS), with live cursor presence and instant sync
- 🤖 **AI Chatbot (RAG)** — Ask questions about your document; the AI answers using only your document's content as context via PGVector similarity search
- 📝 **Rich Text Editor** — Full formatting support (headings, bold, italic, lists, code blocks) powered by TipTap v2
- 🔐 **JWT Authentication** — Secure signup/login with Spring Security 6; all WebSocket connections are authenticated
- 📄 **Document Management** — Create, rename, delete, and organize documents
- 💾 **Auto-Save** — Document changes are persisted automatically to PostgreSQL
- 👥 **User Presence** — See who is currently viewing or editing a document in real time

---

## 🛠️ Tech Stack

### Backend
| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.3.x (Maven multi-module) |
| Security | Spring Security 6 + JWT |
| Real-Time | Spring WebSocket + STOMP protocol |
| Database | PostgreSQL + Spring Data JPA |
| AI / RAG | Spring AI 1.0.x + OpenAI API + PGVector |
| Build | Maven |

### Frontend
| Layer | Technology |
|---|---|
| Framework | React 18 + TypeScript 5 |
| Editor | TipTap v2 |
| State | Zustand (client) + TanStack Query (server) |
| HTTP | Axios with JWT interceptors |
| UI | Tailwind CSS + shadcn/ui |
| Build | Vite 5 |

---

## 🧠 How the RAG Chatbot Works

The AI chatbot does **not** send your entire document to OpenAI. Instead it uses a Retrieval-Augmented Generation (RAG) pipeline:

```
User asks a question
        ↓
Question is converted to a vector embedding (OpenAI)
        ↓
PGVector searches for the most relevant chunks of your document
        ↓
Top-K relevant chunks are passed as context to the LLM
        ↓
OpenAI generates an answer grounded in your document content
```

This makes the chatbot accurate, cost-efficient, and scoped strictly to your document — it won't hallucinate information that isn't there.

---

## 🏗️ Project Structure

```
collabdocs/
├── backend/                        # Spring Boot (Maven multi-module)
│   ├── auth-service/               # JWT auth, user management
│   ├── document-service/           # Document CRUD, WebSocket handlers
│   ├── ai-service/                 # RAG pipeline, embeddings, chat
│   └── common/                     # Shared DTOs, utilities
│
├── frontend/                       # React + TypeScript (Vite)
│   ├── src/
│   │   ├── components/             # UI components (editor, chat, sidebar)
│   │   ├── hooks/                  # Custom React hooks
│   │   ├── store/                  # Zustand state stores
│   │   ├── services/               # Axios API clients
│   │   └── pages/                  # Route-level page components
│   └── vite.config.ts
│
├── .env.example
└── README.md
```

---

## 🚀 Getting Started

### Prerequisites

- Java 21+
- Node.js 18+
- PostgreSQL 15+ (with `pgvector` extension enabled)
- Maven 3.9+
- OpenAI API key

### 1. Clone the repository

```bash
git clone https://github.com/YOUR_USERNAME/collabdocs.git
cd collabdocs
```

### 2. Set up the database

```sql
-- In your PostgreSQL instance:
CREATE DATABASE collabdocs;
\c collabdocs
CREATE EXTENSION vector;
```

### 3. Configure environment variables

```bash
cp .env.example .env
```

Fill in your `.env`:

```env
# Database
DB_URL=jdbc:postgresql://localhost:5432/collabdocs
DB_USERNAME=your_db_user
DB_PASSWORD=your_db_password

# JWT
JWT_SECRET=your_jwt_secret_key_here
JWT_EXPIRATION_MS=86400000

# OpenAI
OPENAI_API_KEY=sk-...

# Frontend
VITE_API_BASE_URL=http://localhost:8080
VITE_WS_URL=ws://localhost:8080/ws
```

### 4. Run the backend

```bash
cd backend
mvn clean install
mvn spring-boot:run
# Server starts on http://localhost:8080
```

### 5. Run the frontend

```bash
cd frontend
npm install
npm run dev
# App starts on http://localhost:5173
```

---

## 🔌 API Overview

### Auth
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register a new user |
| POST | `/api/auth/login` | Login, receive JWT |

### Documents
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/documents` | List all documents for user |
| POST | `/api/documents` | Create a new document |
| GET | `/api/documents/:id` | Get document by ID |
| PUT | `/api/documents/:id` | Update document content |
| DELETE | `/api/documents/:id` | Delete a document |

### AI Chat
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/ai/chat` | Send a message, get AI response scoped to document |

### WebSocket
| Destination | Description |
|-------------|-------------|
| `/ws` | WebSocket handshake endpoint |
| `/app/document.edit` | Publish document changes |
| `/topic/document/:id` | Subscribe to live document updates |
| `/topic/presence/:id` | Subscribe to user presence updates |

---

## 📄 Environment Variables Reference

| Variable | Description | Required |
|----------|-------------|----------|
| `DB_URL` | PostgreSQL JDBC connection URL | ✅ |
| `DB_USERNAME` | Database username | ✅ |
| `DB_PASSWORD` | Database password | ✅ |
| `JWT_SECRET` | Secret key for signing JWTs | ✅ |
| `JWT_EXPIRATION_MS` | JWT expiry in milliseconds | ✅ |
| `OPENAI_API_KEY` | Your OpenAI API key | ✅ |
| `VITE_API_BASE_URL` | Backend base URL (frontend) | ✅ |
| `VITE_WS_URL` | WebSocket URL (frontend) | ✅ |

---

## 🗺️ Roadmap

- [ ] Live cursor positions per user
- [ ] Document sharing with role-based permissions (viewer / editor)
- [ ] Version history and restore
- [ ] Export to PDF / Markdown
- [ ] Deploy with Docker Compose

---

## 👤 Author

**Your Name**
- GitHub: [@Shanvi1228](https://github.com/Shanvi1228)
- LinkedIn: [Shanvi Agnihotri](https://www.linkedin.com/in/shanvi-agnihotri1818/)

---

> ⭐ If you found this project interesting, consider giving it a star — it helps others discover it!
