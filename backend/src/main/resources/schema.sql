CREATE TABLE IF NOT EXISTS tools (
    id VARCHAR(120) PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    description CLOB NOT NULL,
    category VARCHAR(120) NOT NULL,
    provider VARCHAR(160) NOT NULL,
    version VARCHAR(40) NOT NULL,
    risk_level VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL,
    approval_required BOOLEAN NOT NULL,
    parameters_json CLOB NOT NULL,
    schema_json CLOB NOT NULL,
    permission_scopes_json CLOB NOT NULL,
    updated_at VARCHAR(80) NOT NULL
);

CREATE TABLE IF NOT EXISTS prompts (
    id VARCHAR(120) PRIMARY KEY,
    name VARCHAR(180) NOT NULL,
    description CLOB NOT NULL,
    version VARCHAR(40) NOT NULL,
    category VARCHAR(120) NOT NULL,
    status VARCHAR(40) NOT NULL,
    variables_json CLOB NOT NULL,
    usage_scope CLOB NOT NULL,
    related_tools_json CLOB NOT NULL,
    updated_at VARCHAR(80) NOT NULL,
    usage_count INT NOT NULL,
    template_content CLOB NOT NULL
);

CREATE TABLE IF NOT EXISTS resources (
    id VARCHAR(120) PRIMARY KEY,
    name VARCHAR(180) NOT NULL,
    type VARCHAR(60) NOT NULL,
    description CLOB NOT NULL,
    status VARCHAR(40) NOT NULL,
    tags_json CLOB NOT NULL,
    linked_tools_json CLOB NOT NULL,
    updated_at VARCHAR(80) NOT NULL,
    reference_count INT NOT NULL,
    content_summary CLOB NOT NULL,
    schema_preview CLOB NOT NULL,
    markdown_preview CLOB NOT NULL,
    related_prompts_json CLOB NOT NULL
);

CREATE TABLE IF NOT EXISTS tool_calls (
    id VARCHAR(120) PRIMARY KEY,
    tool_id VARCHAR(120) NOT NULL,
    tool_name VARCHAR(180) NOT NULL,
    requester VARCHAR(120) NOT NULL,
    provider VARCHAR(160) NOT NULL,
    environment VARCHAR(80) NOT NULL,
    risk_level VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL,
    request_json CLOB NOT NULL,
    response_json CLOB NOT NULL,
    review_id VARCHAR(120),
    latency VARCHAR(40) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS tool_call_reviews (
    id VARCHAR(120) PRIMARY KEY,
    call_id VARCHAR(120) NOT NULL,
    tool_id VARCHAR(120) NOT NULL,
    risk_level VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL,
    reviewer VARCHAR(120),
    decision VARCHAR(80) NOT NULL,
    comment CLOB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS trace_events (
    id VARCHAR(120) PRIMARY KEY,
    call_id VARCHAR(120) NOT NULL,
    step VARCHAR(80) NOT NULL,
    status VARCHAR(40) NOT NULL,
    message CLOB NOT NULL,
    latency VARCHAR(40) NOT NULL,
    evidence_json CLOB NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id VARCHAR(120) PRIMARY KEY,
    actor VARCHAR(120) NOT NULL,
    action VARCHAR(160) NOT NULL,
    target_type VARCHAR(120) NOT NULL,
    target_id VARCHAR(120) NOT NULL,
    metadata_json CLOB NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS demo_users (
    id VARCHAR(120) PRIMARY KEY,
    username VARCHAR(120) NOT NULL,
    display_name VARCHAR(160) NOT NULL,
    role VARCHAR(40) NOT NULL,
    permission_scopes_json CLOB NOT NULL
);

CREATE TABLE IF NOT EXISTS role_policies (
    id VARCHAR(160) PRIMARY KEY,
    role VARCHAR(40) NOT NULL,
    action VARCHAR(80) NOT NULL,
    allowed BOOLEAN NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_tool_calls_created_at ON tool_calls(created_at);
CREATE INDEX IF NOT EXISTS idx_tool_calls_tool_id ON tool_calls(tool_id);
CREATE INDEX IF NOT EXISTS idx_reviews_call_id ON tool_call_reviews(call_id);
CREATE INDEX IF NOT EXISTS idx_trace_events_call_id ON trace_events(call_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_target ON audit_logs(target_type, target_id);
