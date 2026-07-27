# Layered View

Each module follows:
- domain
- application
- adapter/in
- adapter/out
- config

Dependency rules:
1. Domain has no framework dependency.
2. Application depends on domain and ports.
3. Inbound adapters call application use cases.
4. Outbound adapters implement application ports.
