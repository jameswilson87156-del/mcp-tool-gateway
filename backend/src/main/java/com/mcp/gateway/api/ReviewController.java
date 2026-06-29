package com.mcp.gateway.api;

import com.mcp.gateway.model.CallStatus;
import com.mcp.gateway.model.PageResponse;
import com.mcp.gateway.model.RiskLevel;
import com.mcp.gateway.model.ToolCallReview;
import com.mcp.gateway.service.GatewayService;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/reviews")
public class ReviewController {
    private final GatewayService gateway;

    public ReviewController(GatewayService gateway) {
        this.gateway = gateway;
    }

    @GetMapping
    public PageResponse<ToolCallReview> reviews(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) CallStatus status,
            @RequestParam(required = false) RiskLevel riskLevel,
            @RequestParam(required = false) String toolName,
            @RequestParam(required = false) String keyword
    ) {
        return gateway.listReviews(page, size, status, riskLevel, toolName, keyword);
    }

    @PostMapping("/{id}/approve")
    public ToolCallReview approve(@PathVariable String id, @RequestBody(required = false) ReviewRequest request) {
        return gateway.approveReview(id, request == null ? new ReviewRequest("admin", "") : request);
    }

    @PostMapping("/{id}/reject")
    public ToolCallReview reject(@PathVariable String id, @RequestBody(required = false) ReviewRequest request) {
        return gateway.rejectReview(id, request == null ? new ReviewRequest("admin", "") : request);
    }

    @PostMapping("/{id}/request-changes")
    public ToolCallReview requestChanges(@PathVariable String id, @RequestBody(required = false) ReviewRequest request) {
        return gateway.requestChanges(id, request == null ? new ReviewRequest("admin", "") : request);
    }
}
