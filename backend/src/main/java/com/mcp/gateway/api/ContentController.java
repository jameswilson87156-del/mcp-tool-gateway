package com.mcp.gateway.api;

import com.mcp.gateway.model.PageResponse;
import com.mcp.gateway.model.PromptTemplate;
import com.mcp.gateway.model.ResourceDocument;
import com.mcp.gateway.service.GatewayService;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
public class ContentController {
    private final GatewayService gateway;

    public ContentController(GatewayService gateway) {
        this.gateway = gateway;
    }

    @GetMapping("/api/prompts")
    public PageResponse<PromptTemplate> prompts(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category
    ) {
        return gateway.listPrompts(page, size, keyword, status, category);
    }

    @GetMapping("/api/prompts/{id}")
    public com.mcp.gateway.model.PromptDetail prompt(@PathVariable String id) {
        return gateway.getPromptDetail(id);
    }

    @PostMapping("/api/prompts")
    public com.mcp.gateway.model.PromptDetail createPrompt(@RequestBody PromptUpsertRequest request) {
        return gateway.createPrompt(request);
    }

    @PutMapping("/api/prompts/{id}")
    public com.mcp.gateway.model.PromptDetail updatePrompt(@PathVariable String id, @RequestBody PromptUpsertRequest request) {
        return gateway.updatePrompt(id, request);
    }

    @PostMapping("/api/prompts/{id}/publish")
    public com.mcp.gateway.model.PromptDetail publishPrompt(@PathVariable String id) {
        return gateway.publishPrompt(id);
    }

    @PostMapping("/api/prompts/{id}/archive")
    public com.mcp.gateway.model.PromptDetail archivePrompt(@PathVariable String id) {
        return gateway.archivePrompt(id);
    }

    @PostMapping("/api/prompts/{id}/render")
    public com.mcp.gateway.model.PromptRenderResponse renderPrompt(@PathVariable String id, @RequestBody PromptRenderRequest request) {
        return gateway.renderPrompt(id, request);
    }

    @GetMapping("/api/resources")
    public PageResponse<ResourceDocument> resources(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type
    ) {
        return gateway.listResources(page, size, keyword, status, type);
    }

    @GetMapping("/api/resources/{id}")
    public com.mcp.gateway.model.ResourceDetail resource(@PathVariable String id) {
        return gateway.getResourceDetail(id);
    }

    @PostMapping("/api/resources")
    public com.mcp.gateway.model.ResourceDetail createResource(@RequestBody ResourceUpsertRequest request) {
        return gateway.createResource(request);
    }

    @PutMapping("/api/resources/{id}")
    public com.mcp.gateway.model.ResourceDetail updateResource(@PathVariable String id, @RequestBody ResourceUpsertRequest request) {
        return gateway.updateResource(id, request);
    }

    @PostMapping("/api/resources/{id}/publish")
    public com.mcp.gateway.model.ResourceDetail publishResource(@PathVariable String id) {
        return gateway.publishResource(id);
    }

    @PostMapping("/api/resources/{id}/archive")
    public com.mcp.gateway.model.ResourceDetail archiveResource(@PathVariable String id) {
        return gateway.archiveResource(id);
    }
}
