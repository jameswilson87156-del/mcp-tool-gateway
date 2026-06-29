# Audit Log

P5C adds a lightweight real Audit Log page for demo governance evidence.

## Endpoint

`GET /api/audit-logs?page=&size=&action=&actor=&target=&keyword=`

The endpoint returns `PageResponse<AuditLogEntry>`:

- `items`
- `page`
- `size`
- `total`
- `totalPages`

## UI

The `审计日志` navigation entry opens a real Vue/CSS page with:

- Keyword search.
- Action filter.
- Actor filter.
- Target filter.
- Compact Previous / Next pagination.
- Metadata displayed as scrollable JSON evidence.

## Boundary

This page reads local demo H2 audit rows. It is not production SIEM, full-text search, Elasticsearch, or a real enterprise audit platform.
