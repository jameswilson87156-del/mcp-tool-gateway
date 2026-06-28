package com.mcp.gateway.api;

import com.mcp.gateway.model.PromptTemplate;
import com.mcp.gateway.model.ResourceDocument;
import com.mcp.gateway.service.GatewayService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
public class ContentController {
    private final GatewayService gateway;

    public ContentController(GatewayService gateway) {
        this.gateway = gateway;
    }

    @GetMapping("/api/prompts")
    public List<PromptTemplate> prompts() {
        return gateway.listPrompts();
    }

    @GetMapping("/api/resources")
    public List<ResourceDocument> resources() {
        return gateway.listResources();
    }
}
