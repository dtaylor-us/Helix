# Container View

```mermaid
graph LR
  subgraph Client
    Web[React PWA]
  end
  subgraph Server
    API[Spring Boot Modular Monolith]
  end
  DB[(PostgreSQL)]
  AI[(Optional Local AI Endpoint)]

  Web --> API
  API --> DB
  API --> AI
```
