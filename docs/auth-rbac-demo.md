# Auth / RBAC Demo

The current implementation provides a simple demo user and role model:

- `ADMIN`
- `DEVELOPER`
- `REVIEWER`
- `VIEWER`

`POST /api/auth/login` returns a demo session token and user profile. The token is not a production auth token. RBAC checks are represented as in-memory permission scopes for the P1 workflow and must not be described as production-grade access control.

Permission examples:

- `tool:*`
- `tool:invoke`
- `crm:customer:read`
- `db:query:readonly`
- `review:*`
- `audit:read`
