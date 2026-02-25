# CollabStack Editor — Build & Run Guide (macOS)

## Prerequisites

- **Java 17** (Zulu JDK) at `/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home`
- **Maven 3.9+**
- **PostgreSQL 18** (EDB installer at `/Library/PostgreSQL/18/`)
- **pgvector** extension installed in PostgreSQL
- **Node.js 18+** & npm

---

## Step 1: Database Setup (first time only)

### 1.1 Start PostgreSQL
PostgreSQL runs as a system service via the EDB installer. Verify it's running:
```bash
pg_isready -h localhost -p 5432
```

### 1.2 Create database and user
```bash
# Connect as superuser
psql -U postgres -h localhost

# Inside psql:
CREATE USER collabstack WITH PASSWORD 'Hanu1234';
CREATE DATABASE collabstack_editor OWNER collabstack;
\c collabstack_editor
CREATE EXTENSION IF NOT EXISTS vector;
\q
```

### 1.3 Set JAVA_HOME (add to ~/.zshrc for persistence)
```bash
export JAVA_HOME="/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home"
```

---

## Step 2: Build & Run Backend

### 2.1 Build the JAR
```bash
cd backend
export JAVA_HOME="/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home"
mvn clean install -pl collab-editor -am -DskipTests
```

### 2.2 Run collab-editor (port 8080)
```bash
cd backend/collab-editor
export JAVA_HOME="/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home"
GEMINI_API_KEY=AIzaSyBhWXgiB7aQC1S0_MoSmDGBCTF88kl3BIA \
DB_USERNAME=collabstack \
DB_PASSWORD=Hanu1234 \
java -jar target/collab-editor-0.0.1-SNAPSHOT.jar
```

### 2.3 (Optional) Run cloud-storage (port 8081) — separate terminal
```bash
cd backend/cloud-storage
export JAVA_HOME="/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home"
DB_USERNAME=collabstack \
DB_PASSWORD=Hanu1234 \
java -jar target/cloud-storage-0.0.1-SNAPSHOT.jar
```

### 2.4 (Optional) Run storage nodes — separate terminals each
```bash
cd storage-node
export JAVA_HOME="/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home"

# Node 1 (port 9001)
NODE_ID=node-1 NODE_PORT=9001 DATA_DIR=./data/node1 mvn spring-boot:run

# Node 2 (port 9002) — separate terminal
NODE_ID=node-2 NODE_PORT=9002 DATA_DIR=./data/node2 mvn spring-boot:run

# Node 3 (port 9003) — separate terminal
NODE_ID=node-3 NODE_PORT=9003 DATA_DIR=./data/node3 mvn spring-boot:run
```

---

## Step 3: Run Frontend

### 3.1 Install dependencies (first time only)
```bash
cd frontend/collab-editor
npm install
```

### 3.2 Start dev server (port 5173)
```bash
cd frontend/collab-editor
npm run dev
```

---

## Quick Start (after first-time setup)

**Terminal 1 — Backend:**
```bash
export JAVA_HOME="/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home"
cd backend/collab-editor
GEMINI_API_KEY=AIzaSyBhWXgiB7aQC1S0_MoSmDGBCTF88kl3BIA DB_USERNAME=collabstack DB_PASSWORD=Hanu1234 java -jar target/collab-editor-0.0.1-SNAPSHOT.jar
```

**Terminal 2 — Frontend:**
```bash
cd frontend/collab-editor
npm run dev
```

**Access:** http://localhost:5173