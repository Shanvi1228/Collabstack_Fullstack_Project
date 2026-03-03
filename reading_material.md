# 📚 CollabStack Full-Stack Project - Revision Notes

## 🏗️ Project Overview
**CollabStack** = Two apps in one project:
- **collab-editor** (Google Docs clone) - Real-time document editing + AI chat
- **cloud-storage** (Dropbox clone) - Encrypted file storage with fault tolerance

---

## 📁 Project Structure (Monorepo)

```
/
├── backend/                    # 🍳 Kitchen (Java Spring Boot)
│   ├── collab-editor/         # Document service (port 8080)
│   └── cloud-storage/         # File storage service (port 8081)
├── storage-node/              # 📦 Warehouse workers (port 9001-9003)
├── frontend/                  # 🎨 Customer area (React)
│   ├── collab-editor/         # Document editing UI
│   └── cloud-storage/         # File management UI
└── docker-compose.yml         # 🐳 Runs everything together
```

---

## 🏛️ Architecture Pattern: Layered Architecture

**Think of it like a restaurant:**
```
🍽️ Controller  (Waiter)     → Takes orders, serves food
🧠 Service     (Chef)       → Business logic, cooking rules  
📦 Repository  (Storage)    → Gets ingredients from storage
🗄️ Entity      (Recipe)     → Defines what food looks like
📋 DTOs        (Menu)       → What customers see/order
```

**Every service follows the same pattern:**
- Frontend talks to Controller
- Controller calls Service  
- Service uses Repository
- Repository talks to Database

---

## 🔧 Backend File Creation Order (SACRED ORDER!)

**When adding ANY new feature, ALWAYS create files in this order:**

### 1️⃣ **Entity** (The Blueprint) - FIRST
```java
@Entity public class Comment {
    @Id private UUID id;
    private String content;
    private UUID documentId;
}
```

### 2️⃣ **Repository** (The Storage Worker) - SECOND  
```java
interface CommentRepository extends JpaRepository<Comment, UUID> {
    List<Comment> findByDocumentId(UUID docId);
}
```

### 3️⃣ **DTOs** (The Order Forms) - THIRD
```java
record CommentRequest(String content, UUID documentId) {}
record CommentResponse(UUID id, String content, String author) {}
```

### 4️⃣ **Service Interface** (The Contract) - FOURTH
```java
interface CommentService {
    CommentResponse createComment(UUID userId, CommentRequest request);
}
```

### 5️⃣ **Service Implementation** (The Worker) - FIFTH
```java
@Service class CommentServiceImpl implements CommentService {
    // All the business logic goes here
}
```

### 6️⃣ **Mapper** (The Translator) - SIXTH
```java
@Mapper interface CommentMapper {
    CommentResponse toResponse(Comment comment);
}
```

### 7️⃣ **Controller** (The Front Desk) - SEVENTH
```java
@RestController class CommentController {
    @PostMapping("/api/comments") 
    ResponseEntity<ApiResponse<CommentResponse>> create(...) {}
}
```

**🚫 NEVER skip this order! Each step needs the previous one to exist.**

---

## 🌐 API Endpoints Logic - Complete Reference

### 🔐 **Authentication (Front Desk)**

| Endpoint | Logic | Input | Output |
|----------|-------|-------|---------|
| `POST /api/auth/register` | Check if email exists → Hash password → Save user → Generate JWT | email, username, password | JWT token + user info |
| `POST /api/auth/login` | Find user by email → Check password → Generate JWT | email, password | JWT token + user info |

### 📄 **Documents (Library Service)**

| Endpoint | Logic | Permission | What it does |
|----------|-------|------------|--------------|
| `GET /api/documents` | Find all docs owned/shared with user | Any user | Show my document list |
| `POST /api/documents` | Create new doc → Set owner → Save | Any user | Create blank document |
| `GET /api/documents/{id}` | Find doc → Check access → Return content | Owner or Collaborator | Open specific document |
| `PUT /api/documents/{id}/title` | Find doc → Check owner → Update title | Owner only | Rename document |
| `DELETE /api/documents/{id}` | Find doc → Check owner → Delete all data | Owner only | Delete document forever |
| `POST /api/documents/{id}/collaborators` | Find doc → Check owner → Add user as collaborator | Owner only | Invite someone to edit |
| `GET /api/documents/{id}/collaborators` | Find doc → Check access → List all collaborators | Owner or Collaborator | See who can edit |
| `POST /api/documents/{id}/share` | Check owner → Generate magic token → Set expiry | Owner only | Create sharing link |
| `POST /api/documents/join/{token}` | Find valid token → Add user as collaborator → Return doc | Any user | Join via magic link |

### 🤖 **AI Chat (Smart Assistant)**

| Endpoint | Logic | What it does |
|----------|-------|--------------|
| `POST /api/documents/{id}/chat` | Check access → Split doc into chunks → Find relevant chunks → Ask AI → Return answer | Ask questions about document |
| `POST /api/documents/{id}/index` | Check access → Re-chunk document → Update AI memory | Update AI's knowledge of document |

### 📁 **File Storage (Bank Vault)**

| Endpoint | Logic | What it does |
|----------|-------|--------------|
| `POST /api/files/upload` | Generate encryption key → Split into chunks → Store in 3 locations → Save metadata | Store file securely |
| `GET /api/files` | Find user's files → Check health of each file | List my stored files |
| `GET /api/files/{id}` | Check ownership → Collect chunks → Decrypt with password → Rebuild file | Download my file |
| `DELETE /api/files/{id}` | Check ownership → Delete all chunks from storage nodes → Delete metadata | Delete file forever |

### 🔧 **Admin (Management Office)**

| Endpoint | Logic | What it does |
|----------|-------|--------------|
| `GET /api/admin/nodes` | List all storage nodes → Check health status | See storage infrastructure |
| `POST /api/admin/nodes` | Test connection → Register new node | Add new storage building |
| `GET /api/admin/files/{id}/chunks` | Find file → Map chunk locations → Show distribution | See where file pieces are stored |
| `POST /api/admin/repair` | Find broken chunks → Copy from healthy nodes → Fix replication | Repair damaged storage |

### 📦 **Storage Nodes (Warehouse Workers)**

| Endpoint | Logic | What it does |
|----------|-------|--------------|
| `PUT /chunks/{id}` | Save encrypted data to local disk | Store file piece |
| `GET /chunks/{id}` | Read encrypted data from local disk | Retrieve file piece |
| `DELETE /chunks/{id}` | Delete file from local disk | Remove file piece |
| `GET /chunks/health` | Count stored chunks → Check disk space → Report status | Health check |

---

## 🔒 Security Rules (Applied Everywhere)

1. **🎫 Authentication**: Must have valid JWT token (except register/login)
2. **🔒 Authorization**: Can only access your own data (except shared documents)  
3. **👑 Ownership**: Only owners can delete/modify documents
4. **🔐 Encryption**: All files encrypted with user password
5. **🧂 Hashing**: All passwords hashed with BCrypt

---

## 💾 Technology Stack Summary

### Backend (Java)
- **Framework**: Spring Boot 3.3.x
- **Security**: Spring Security 6 + JWT
- **Database**: PostgreSQL + Spring Data JPA
- **WebSocket**: Spring WebSocket + STOMP
- **AI**: Spring AI 1.0.x + OpenAI + PGVector
- **Build**: Maven multi-module

### Frontend (React)  
- **Framework**: React 18 + TypeScript 5
- **Editor**: TipTap v2 (rich text editing)
- **State**: Zustand (client) + TanStack Query (server)
- **HTTP**: Axios with JWT interceptors
- **UI**: Tailwind CSS + shadcn/ui
- **Build**: Vite 5

---

## 🎯 Key Concepts to Remember

### **Operational Transform (OT)**
- Multiple users edit same document
- Conflicts resolved automatically  
- Server broadcasts changes to all clients
- Like having a smart referee for simultaneous edits

### **RAG (AI Chat)**
- Split document into chunks
- Convert to numbers (embeddings)
- Find relevant chunks for user question
- AI answers using only document content

### **Distributed Storage**
- Files split into chunks (4MB each)
- Each chunk stored in 3 different locations
- If one location fails, file still accessible
- Automatic repair restores missing copies

### **Encryption**
- Each file has unique encryption key (DEK)
- DEK encrypted with user password (KEK)
- Storage nodes only see encrypted data
- Only user with password can decrypt

---

## 📝 Quick Reference Commands

### Start Everything
```bash
docker-compose up  # Starts all services
```

### API Testing
- **Editor**: http://localhost:8080/swagger-ui.html
- **Storage**: http://localhost:8081/swagger-ui.html

### Database Access
```bash
docker exec -it postgres psql -U collabstack -d collabstack_editor
docker exec -it postgres psql -U collabstack -d collabstack_storage  
```

---

## 🎓 Study Tips

1. **Remember the order**: Entity → Repository → DTOs → Service → Controller
2. **Think in layers**: Each layer only talks to the layer below
3. **Security first**: Every endpoint checks authentication & authorization
4. **Follow patterns**: All endpoints follow same validation → business logic → response pattern
5. **Understand data flow**: Frontend → Controller → Service → Repository → Database

**🔥 Pro Tip**: If you understand this architecture, you can build ANY backend feature by following the same pattern!
