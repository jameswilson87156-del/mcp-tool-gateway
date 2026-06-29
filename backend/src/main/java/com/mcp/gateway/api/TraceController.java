package com.mcp.gateway.api;

import com.mcp.gateway.model.CallStatus;
import com.mcp.gateway.model.PageResponse;
import com.mcp.gateway.model.RiskLevel;
import com.mcp.gateway.model.TraceDetail;
import com.mcp.gateway.model.TraceSummary;
import com.mcp.gateway.security.PolicyAction;
import com.mcp.gateway.security.PolicyService;
import com.mcp.gateway.service.GatewayService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/traces")
public class TraceController {
    private final GatewayService gateway;
    private final PolicyService policy;

    public TraceController(GatewayService gateway, PolicyService policy) {
        this.gateway = gateway;
        this.policy = policy;
    }

    @GetMapping
    public PageResponse<TraceSummary> traces(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) CallStatus status,
            @RequestParam(required = false) RiskLevel riskLevel,
            @RequestParam(required = false) String toolName,
            @RequestParam(required = false) Boolean reviewRequired,
            @RequestParam(required = false) String keyword,
            @RequestHeader(value = "X-Demo-Role", required = false) String demoRole
    ) {
        policy.require(demoRole, PolicyAction.TRACE_VIEW);
        return gateway.listTraceSummaries(page, size, status, riskLevel, toolName, reviewRequired, keyword);
    }

    @GetMapping("/{traceId}")
    public TraceDetail traceDetail(
            @PathVariable String traceId,
            @RequestHeader(value = "X-Demo-Role", required = false) String demoRole
    ) {
        policy.require(demoRole, PolicyAction.TRACE_VIEW);
        return gateway.getTraceDetail(traceId);
    }
}
