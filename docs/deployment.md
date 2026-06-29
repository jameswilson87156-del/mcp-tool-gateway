# Local Deployment / 本地部署

P7A 提供 Docker Compose 一键启动，用于本地运行和 GitHub 项目演示。它不是生产部署方案。

## Prerequisites

- Docker Engine 或 Docker Desktop。
- Docker Compose v2（使用 `docker compose` 命令）。
- 本地端口 `8080` 和 `8088` 可用。

## Start

在仓库根目录运行：

```bash
docker compose up --build
```

可访问：

- Frontend：`http://localhost:8088`
- Backend API：`http://localhost:8080/api`
- Swagger UI：`http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON：`http://localhost:8080/v3/api-docs`

停止并移除本次 demo 容器：

```bash
docker compose down
```

## Container Layout

- `backend/Dockerfile` 使用 Maven + Java 17 多阶段构建 Spring Boot jar，运行阶段使用 Java 17 JRE 和非 root 用户。
- `frontend/Dockerfile` 使用 Node 20 构建 Vue/Vite `dist`，运行阶段由 Nginx 提供静态页面。
- Frontend 在镜像构建时使用 `http://localhost:8080/api` 作为浏览器 API 地址。
- Compose 不挂载数据库 volume，也不需要 `.env` 或真实凭据。

## Persistence Boundary

Backend 继续使用默认 H2 in-memory demo persistence。容器重启或重建后，数据会由 seed-on-empty 逻辑重新初始化。

这不是 MySQL / PostgreSQL、持久卷、备份恢复或高可用数据库方案。不要把 demo 数据视为真实企业数据。

## Deployment Boundary

当前 Compose 仅用于本地 demo，不包含：

- 生产级 HTTPS / TLS termination。
- OAuth、SSO、JWT 或可信身份系统。
- WAF、API gateway、rate limit 或 production CORS policy。
- Multi-tenant isolation。
- Secret manager 或真实外部 Tool credentials。
- Kubernetes、自动扩缩容、监控告警、备份与灾备。

Tool execution 和 Prompt render 仍是 demo/sandbox；OpenAPI 只描述当前 `/api/**` REST demo endpoints，不代表完整 MCP 官方协议。

## Validation

```bash
docker compose config
docker compose build
```

如果本机没有可用 Docker daemon，`docker compose config` 仍可用于验证 Compose 结构；镜像构建结果应由 GitHub 或具备 Docker Engine 的环境再次确认。
