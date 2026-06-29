# Literature Review System with AI Assistance

An advanced, full-stack web application designed to streamline the academic literature review workflow. The system enables assigned reviewers to efficiently evaluate research papers within targeted surveys, tracking review progress in real time while utilizing **Google Gemini** via API calls to generate smart annotations, queries, and data extraction suggestions.

---

## Key Features

* **Survey-Based Dashboard:** View and manage assignments from a dedicated review inbox.
* **Interactive Evaluation Workspace:** Split-screen layout to cross-reference article abstracts and structural metadata side-by-side.
* **Progress Tracking Metrics:** Visual progress indicators reflecting the exact completion status of a survey's pipeline.
* **Gemini AI Integration:** On-demand AI suggestions and query extractions powered by the Gemini API to help accelerate validation.
* **State Preservation:** Save review remarks, timestamps, and approval/denial states directly back to the database.

---

## Architecture & Tech Stack

* **Backend:** Java / Spring Boot
* **Frontend:** React.js, TypeScript, Tailwind CSS, Lucide Icons, Framer Motion
* **Database:** Managed via Docker Environment
* **AI Engine:** Google Gemini API (via HTTP client requests)

---

## Getting Started & Installation

Follow these steps sequentially to set up your local development environment.

### 1. Prerequisites
Ensure you have the following software installed on your machine:
* [Docker & Docker Compose](https://www.docker.com/)
* [Node.js (v18+) & npm](https://nodejs.org/)
* [Java Development Kit (JDK 17+)](https://adoptium.net/)
* A valid **Gemini API Key** configured in your backend environment parameters.

### 2. Database Setup (Docker)
The application utilizes a Docker container configuration to spin up its data layer. From the root directory of the project, execute:

```bash
docker compose up -d
```

### 3. Frontend Setup
Navigate into the dedicated web client layout directory, build the assets, and start the development server:

```bash
# Navigate to frontend folder
cd frontend

# Install dependencies
npm install

# Build the application
npm run build

# Start the local vite development server
npm run dev
```
Once initialized, the terminal will provide a local address (usually http://localhost:5173) to view the application interface.

### 4. Backend Setup (Spring Boot)
Open a new terminal window, navigate back to the root directory where the Spring Boot project lives, and initialize the application runner:
```bash
./mvnw spring-boot:run
```

The backend server will hook directly into your active Docker database container and expose its REST endpoints for the frontend to consume.

## Live Demo
You can interact with a live environment of the platform directly here:
[https://www.youtube.com/watch?v=tekRV-I-Jj8](https://www.youtube.com/watch?v=tekRV-I-Jj8)
