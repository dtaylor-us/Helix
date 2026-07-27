# Architecture Vision

Helix uses a modular monolith backend and a separate web client.

Key goals:
- Preserve domain boundaries and evolvability.
- Ensure browser-to-backend-only data access.
- Keep AI optional and adapter-based.
- Optimize for single-user privacy and longitudinal integrity.
