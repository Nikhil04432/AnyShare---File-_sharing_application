# AnyShare — Secure P2P File Sharing Application

## 🚀 Overview

AnyShare is a real-time, secure, peer-to-peer (P2P) file-sharing application built using **Spring Boot, WebRTC, and React**. The system enables users to share files directly between devices without routing the actual file data through a central server, ensuring faster transfers and better privacy.

The backend is responsible for:
- Session management  
- Peer coordination  
- JWT-based authentication and authorization  
- WebSocket-based signaling  
- Persistent session storage using PostgreSQL  

The frontend is responsible for:
- User authentication  
- Session creation and joining  
- WebRTC peer connection setup  
- Real-time file transfer via WebRTC Data Channels  

---

## 🎯 Key Features

- ✅ Direct P2P file transfer using WebRTC  
- ✅ No file storage on the server  
- ✅ Secure authentication using JWT  
- ✅ Real-time signaling via WebSockets  
- ✅ Room-code based session system  
- ✅ PostgreSQL for session persistence  
- ✅ Fully Dockerized backend and frontend  
- ✅ Deployed on cloud infrastructure  
- ✅ Proper CORS configuration for security  

---

## 🏗️ System Architecture

### High-Level Flow

1. **User creates a session**
   - Backend generates a unique session ID and room code.
   - Session is stored in PostgreSQL and cached in memory.

2. **Another user joins using room code**
   - Backend validates the session and assigns roles (SENDER / RECEIVER).
   - A JWT token is issued for secure WebSocket communication.

3. **WebRTC connection setup**
   - Peers exchange SDP offers and answers via WebSocket signaling.
   - ICE candidates are exchanged to establish a direct P2P connection.

4. **File Transfer**
   - Once the WebRTC Data Channel is open, files are streamed directly between peers.

---

## 🛠️ Tech Stack

### Backend
- Java 17  
- Spring Boot  
- Spring Security (JWT)  
- WebSockets  
- Hibernate + JPA  
- PostgreSQL  
- Liquibase  
- Maven  
- Docker  

### Frontend
- React  
- WebRTC  
- WebSockets  
- Zustand (State Management)  
- Axios  
- Vite  

---

## 📁 Project Structure
AnyShare/
│── backend/ # Spring Boot backend
│── frontend/ # React frontend
│── docker-compose.yml
│── README.md


---

## 🔐 Security

- Stateless JWT authentication  
- Secure WebSocket communication  
- Restricted CORS policy  
- No server-side file storage  

---

## 🧪 Testing

Backend is tested using:
- JUnit 5  
- Mockito  
- MockMvc  
- Service layer unit tests (95%+ coverage target)  

---

## 🚀 Deployment

- **Backend:** Render  
- **Frontend:** Vercel  
- **Database:** Neon PostgreSQL  

---

## ▶️ How to Run Locally

### 1. Clone the repository
git clone https://github.com/Nikhil04432/AnyShare---File-_sharing_application.git
cd AnyShare---File-_sharing_application

### 2. Run Backend (Docker Recommended)
docker-compose up --build

### 3. Run Frontend
cd frontend
npm install
npm run dev

