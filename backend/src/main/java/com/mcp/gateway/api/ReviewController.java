package com.mcp.gateway.api;

import com.mcp.gateway.model.ToolCallReview;
import com.mcp.gateway.service.GatewayService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/reviews")
public class ReviewController {
    private final GatewayService gateway;

    public ReviewController(GatewayService gateway) {
        this.gateway = gateway;
    }

    @GetMapping
    public List<ToolCallReview> reviews() {
        return gateway.listReviews();
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
