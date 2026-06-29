# Persistence

P5A adds H2 + JdbcTemplate persistence for the MCP Tool Gateway demo.

P5B uses the same repository boundary for Prompt / Resource create, update, publish, and archive workflows.

P5C adds service-layer filtering and pagination for local demo governance lists.

P5D uses the same `role_policies` demo table through `PolicyService` for sensitive API checks.

## Storage

The backend uses Spring JDBC repositories under `backend/src/main/java/com/mcp/gateway/persistence`.

Tables are created by `backend/src/main/resources/schema.sql`:

- `tools`
- `prompts`
- `resources`
- `tool_calls`
- `tool_call_reviews`
- `trace_events`
- `audit_logs`
- `demo_users`
- `role_policies`

## JSON Fields

Complex fields are stored as JSON strings in `CLOB` columns and are encoded/decoded by `JsonCodec`.

Examples:

- Tool parameters, schema, and permission scopes.
- Tool call request and response JSON.
- Trace evidence metadata.
- Audit metadata.
- Prompt variables and related Tools.
- Resource tags, linked Tools, and related Prompts.

This keeps P5A intentionally small and avoids over-normalizing the demo data model.

## Seed Strategy

`GatewayService.seedIfEmpty()` checks the repository state on startup.

If the `tools` table is empty, it seeds the P1-P4-compatible demo data for Tools, Prompts, Resources, a pending Review, Trace Events, Audit Logs, demo users, and role policy demo rows.

If the tables already contain data, seed does not duplicate the main demo rows.

## Runtime Boundary

The default configuration uses H2 in-memory storage:

`jdbc:h2:mem:mcp_tool_gateway;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1`

Local file-backed H2 can be configured for manual development, but generated database files must not be committed:

- `.data/`
- `*.mv.db`
- `*.trace.db`

## Limitations

This is a local demo persistence layer, not a production database architecture.

Prompt / Resource write operations persist to the local H2 demo store and write `AuditLogEntry` rows. They do not make the project a real enterprise configuration center.

P5C pagination and filtering are designed for the seeded/local H2 demo data set. They are not full-text search, not Elasticsearch, and not production-grade search infrastructure.

PolicyService reads local demo role policy rows. It does not provide OAuth, SSO, JWT, multi-tenant authorization, or production account management.

It does not add production-grade permissions, complete official MCP protocol compatibility, real provider execution, real enterprise data, unattended high-risk execution, or an enterprise knowledge graph.

Future production-like work could migrate the same repository boundary toward MySQL or PostgreSQL, add migrations, replace the demo header with real identity, and move authorization decisions into a production security layer.
