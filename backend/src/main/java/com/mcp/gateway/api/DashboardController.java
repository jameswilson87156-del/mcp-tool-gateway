package com.mcp.gateway.api;

import com.mcp.gateway.service.GatewayService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final GatewayService gateway;

    public DashboardController(GatewayService gateway) {
        this.gateway = gateway;
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return gateway.dashboardStats();
    }
}
