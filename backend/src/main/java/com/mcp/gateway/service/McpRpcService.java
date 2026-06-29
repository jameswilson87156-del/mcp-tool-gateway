package com.mcp.gateway.service;

import com.mcp.gateway.api.InvokeRequest;
import com.mcp.gateway.model.CallStatus;
import com.mcp.gateway.model.McpRpcRequest;
import com.mcp.gateway.model.McpRpcResponse;
import com.mcp.gateway.security.PolicyAccessDeniedException;
import com.mcp.gateway.security.PolicyAction;
import com.mcp.gateway.security.PolicyService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
public class McpRpcService {
    private static final int INVALID_REQUEST = -32600;
    private static final int METHOD_NOT_FOUND = -32601;
    private static final int INVALID_PARAMS = -32602;
    private static final int SERVER_ERROR = -32000;
    private static final int FORBIDDEN = -32003;

    private final GatewayService gateway;
    private final PolicyService policy;

    public McpRpcService(GatewayService gateway, PolicyService policy) {
        this.gateway = gateway;
        this.policy = policy;
    }

    public McpRpcResponse handle(McpRpcRequest request) {
        var id = request == null ? null : request.id();
        if (request == null || !"2.0".equals(request.jsonrpc()) || request.method() == null || request.method().isBlank()) {
            return error(id, INVALID_REQUEST, "Invalid Request", Map.of());
        }

        try {
            return switch (request.method()) {
                case "tools/list" -> listTools(id);
                case "tools/call" -> callTool(id, request.params());
                case "prompts/list" -> listPrompts(id);
                case "resources/list" -> listResources(id);
                default -> error(id, METHOD_NOT_FOUND, "Method not found", Map.of());
            };
        } catch (InvalidParamsException exception) {
            return error(id, INVALID_PARAMS, "Invalid params", Map.of());
        } catch (PolicyAccessDeniedException exception) {
            return error(id, FORBIDDEN, "Forbidden", Map.of(
                    "action", exception.action().name(),
                    "role", exception.role().name(),
                    "requestId", exception.requestId()
            ));
        } catch (RuntimeException exception) {
            return error(id, SERVER_ERROR, "Server error", Map.of());
        }
    }

    private McpRpcResponse listTools(Object id) {
        var tools = gateway.listTools().stream().map(tool -> {
            var summary = new LinkedHashMap<String, Object>();
            summary.put("name", tool.name());
            summary.put("description", tool.description());
            summary.put("schema", tool.schema());
            summary.put("riskLevel", tool.riskLevel());
            summary.put("provider", tool.provider());
            summary.put("approvalRequired", tool.approvalRequired());
            return summary;
        }).toList();
        return McpRpcResponse.success(id, Map.of("tools", tools));
    }

    private McpRpcResponse listPrompts(Object id) {
        var prompts = gateway.listPrompts(0, 50, null, null, null).items().stream().map(prompt -> {
            var summary = new LinkedHashMap<String, Object>();
            summary.put("name", prompt.name());
            summary.put("description", prompt.description());
            summary.put("version", prompt.version());
            summary.put("status", prompt.status());
            summary.put("variables", prompt.variables());
            return summary;
        }).toList();
        return McpRpcResponse.success(id, Map.of("prompts", prompts));
    }

    private McpRpcResponse listResources(Object id) {
        var resources = gateway.listResources(0, 50, null, null, null).items().stream().map(resource -> {
            var summary = new LinkedHashMap<String, Object>();
            summary.put("name", resource.name());
            summary.put("type", resource.type());
            summary.put("status", resource.status());
            summary.put("description", resource.description());
            summary.put("linkedTools", resource.linkedTools());
            return summary;
        }).toList();
        return McpRpcResponse.success(id, Map.of("resources", resources));
    }

    private McpRpcResponse callTool(Object id, Map<String, Object> params) {
        if (params == null) {
            throw new InvalidParamsException();
        }
        var toolName = requiredString(params, "toolName");
        var arguments = requiredArguments(params);
        var role = optionalString(params, "role");
        var tool = gateway.listTools().stream()
                .filter(candidate -> candidate.name().equals(toolName) || candidate.id().equals(toolName))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Tool not found: " + toolName));

        policy.require(role, PolicyAction.TOOL_INVOKE);
        var call = gateway.invoke(tool.id(), new InvokeRequest("sandbox", "mcp-json-rpc", arguments));
        var payload = new LinkedHashMap<String, Object>();
        payload.put("callId", call.id());
        payload.put("status", call.status());
        payload.put("reviewRequired", call.reviewId() != null);
        payload.put("traceId", gateway.getTraceDetail(call.id()).traceId());

        if (call.status() == CallStatus.PENDING_REVIEW) {
            payload.put("pendingReview", Map.of(
                    "reviewId", call.reviewId(),
                    "message", "Human Review required before sandbox execution"
            ));
        } else {
            payload.put("result", call.response() == null ? Map.of() : call.response());
        }
        return McpRpcResponse.success(id, payload);
    }

    private String requiredString(Map<String, Object> params, String field) {
        var value = params.get(field);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new InvalidParamsException();
        }
        return text;
    }

    private String optionalString(Map<String, Object> params, String field) {
        if (!params.containsKey(field) || params.get(field) == null) {
            return null;
        }
        return requiredString(params, field);
    }

    private Map<String, Object> requiredArguments(Map<String, Object> params) {
        if (!(params.get("arguments") instanceof Map<?, ?> values)) {
            throw new InvalidParamsException();
        }
        var arguments = new LinkedHashMap<String, Object>();
        values.forEach((key, value) -> {
            if (!(key instanceof String text)) {
                throw new InvalidParamsException();
            }
            arguments.put(text, value);
        });
        return arguments;
    }

    private McpRpcResponse error(Object id, int code, String message, Map<String, Object> data) {
        return McpRpcResponse.failure(id, code, message, data);
    }

    private static class InvalidParamsException extends RuntimeException {
    }
}
