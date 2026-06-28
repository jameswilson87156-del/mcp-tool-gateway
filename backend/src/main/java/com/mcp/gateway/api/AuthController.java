package com.mcp.gateway.api;

import com.mcp.gateway.model.UserAccount;
import com.mcp.gateway.service.GatewayService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/auth")
public class AuthController {
    private final GatewayService gateway;

    public AuthController(GatewayService gateway) {
        this.gateway = gateway;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody(required = false) LoginRequest request) {
        UserAccount user = gateway.login(request == null ? "admin" : request.username());
        return Map.of("user", user, "token", "demo-session-token", "demo", true);
    }

    @GetMapping("/me")
    public UserAccount me() {
        return gateway.me();
    }
}
