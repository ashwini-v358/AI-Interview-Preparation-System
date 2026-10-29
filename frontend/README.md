# Smart Interview Platform Frontend

React 19, TypeScript, and Vite client for the existing Spring Boot API. The client includes authentication, resume PDF upload and analysis, pasted job-description analysis, and REST-based interview sessions.

## Run Locally

Start the backend on port 8080, then from this directory run:

```powershell
npm install
npm run dev
```

The API origin defaults to `http://localhost:8080`. To use another backend, set `VITE_API_BASE_URL` to its origin (without an `/api` suffix) before starting Vite. For example, in PowerShell:

```powershell
$env:VITE_API_BASE_URL = "http://localhost:8080"
npm run dev
```

## Checks

```powershell
npm run build
npm run lint
```

Swagger UI is linked from the app navigation using the configured API origin. Skill-gap analysis and reports are not shown as active workflows because the current backend does not expose those APIs.
