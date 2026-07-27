# Model Provider Contract

Input contract:
- Model name
- Endpoint URI
- Timeout and cancellation support
- Prompt version identifier

Output contract:
- Structured payload validated against schema
- Provider/model metadata
- Confidence/uncertainty markers where applicable

Failure contract:
- Timeout, invalid response, refusal, unavailable provider all mapped to explicit app outcomes.
