# AI and Memory View

AI architecture pattern:
- Application-level AI port
- Adapter implementations: no-AI fallback, local Ollama-compatible endpoint, future external provider

Memory governance principle:
- AI-derived content remains proposed until explicitly accepted.
- Memory proposals carry source provenance, can be revised or rejected, and are deleted only by user choice.
