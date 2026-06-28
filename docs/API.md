# API

Base URL: `http://localhost:8080/api`

## Auth

- `POST /auth/login`
- `GET /auth/me`

## Tools

- `GET /tools`
- `GET /tools/{id}`
- `POST /tools/{id}/invoke`

Invoke request:

```json
{
  "environment": "production",
  "requester": "admin",
  "parameters": {
    "customer_id": "CUST-202405-000123",
    "region": "APAC",
    "limit": 20
  }
}
```

## Tool Calls

- `GET /tool-calls`
- `GET /tool-calls/{id}`
- `GET /tool-calls/{id}/trace`

## Reviews

- `GET /reviews`
- `POST /reviews/{id}/approve`
- `POST /reviews/{id}/reject`
- `POST /reviews/{id}/request-changes`

## Prompts / Resources / Dashboard

- `GET /prompts`
- `GET /resources`
- `GET /dashboard/stats`

## Demo Tools

- `weather.lookup`
- `ticket.search`
- `resume.analyze`
- `github.issue.search`
- `db.query.readonly`
- `crm.customer.search`
