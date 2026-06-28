# Project Plan

## P1 Goal

Build a runnable MCP-style enterprise Agent tool gateway demo with a Spring Boot backend, Vue 3 frontend, and real browser screenshots.

## Scope

- Tool / Prompt / Resource demo registry.
- Tool Schema and JSON invocation workflow.
- Risk classification.
- Human Review for high-risk tool calls.
- Tool Call Trace and Audit Log events.
- B2 Tool Call Workbench as the default homepage.

## Boundaries

- MCP-style only; not a complete official MCP protocol implementation.
- RBAC is demo-only; not a production-grade permission system.
- Tool execution is sandbox/demo logic only.
- `db.query.readonly` only permits local demo `SELECT`.
- Human Review requires explicit review actions; no unattended production execution.
