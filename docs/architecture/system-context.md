# System Context

```mermaid
graph TD
  User[Single Helix User] --> Web[Helix Web PWA]
  Web --> Api[Helix API Modular Monolith]
  Api --> Pg[(PostgreSQL)]
  Api --> Ollama[Optional Ollama-Compatible Endpoint]
```
