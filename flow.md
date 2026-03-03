

## Backend — collab-editor

### Prerequisites (one-time)
- Java 17+
- Maven 3.9+
- PostgreSQL running on port 5432

### 1. Database Setup (one-time only)

Connect as the `postgres` superuser:
```powershell
$env:PGPASSWORD="Bluegreen18@"; psql -U postgres -h localhost
```

Inside psql:
```sql
CREATE USER collabstack WITH PASSWORD 'Bluegreen18@';
CREATE DATABASE collabstack_editor OWNER collabstack;
\c collabstack_editor
CREATE EXTENSION IF NOT EXISTS vector;

-- Grant all permissions to collabstack user
GRANT ALL PRIVILEGES ON DATABASE collabstack_editor TO collabstack;
GRANT ALL PRIVILEGES ON SCHEMA public TO collabstack;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO collabstack;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO collabstack;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO collabstack;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO collabstack;
\q
```

### 2. Build ~
```powershell
cd C:\my_Backend_Project\backend
mvn clean install -pl collab-editor -am -DskipTests
```

### 3. Run
```powershell
cd C:\my_Backend_Project\backend\collab-editor

$env:GEMINI_API_KEY="AIzaSyBhWXgiB7aQC1S0_MoSmDGBCTF88kl3BIA"
$env:DB_USERNAME="collabstack"
$env:DB_PASSWORD="Bluegreen18@"

java -jar target/collab-editor-0.0.1-SNAPSHOT.jar
```

> **Port conflict?** If port 8080 is busy, add `$env:SERVER_PORT="8081"` before the `java` command.

Backend runs at: **http://localhost:8080**  
Swagger UI at: **http://localhost:8080/swagger-ui.html**

---

## Frontend — collab-editor

### Prerequisites (one-time)
- Node.js 18+
- npm

### 1. Install Dependencies (one-time only)
```powershell
cd C:\my_Backend_Project\frontend\collab-editor
npm install
```

### 2. Configure Environment
The .env.local file already exists with correct values. Verify it matches your backend port:
```env
VITE_API_BASE_URL=http://localhost:8080/api
VITE_WS_BASE_URL=ws://localhost:8080
```
> If you started the backend on port **8081**, change both values to `8081`.

### 3. Run (dev server)
```powershell
cd C:\my_Backend_Project\frontend\collab-editor
npm run dev
```

Frontend runs at: **http://localhost:5173**

### 4. Build for Production (optional)
```powershell
npm run build
```
Output goes to `frontend/collab-editor/dist/`.

---

## Quick Start (subsequent runs)

**Terminal 1 — Backend:**
```powershell
cd C:\my_Backend_Project\backend\collab-editor
$env:GEMINI_API_KEY="AIzaSyBhWXgiB7aQC1S0_MoSmDGBCTF88kl3BIA"
$env:DB_USERNAME="collabstack"
$env:DB_PASSWORD="Bluegreen18@"
java -jar target/collab-editor-0.0.1-SNAPSHOT.jar
```

**Terminal 2 — Frontend:**
```powershell
cd C:\my_Backend_Project\frontend\collab-editor
npm run dev
```

**Access:** http://localhost:5173