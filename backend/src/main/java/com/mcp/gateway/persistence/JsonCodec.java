package com.mcp.gateway.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class JsonCodec {
    private final ObjectMapper mapper;

    public JsonCodec(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public String write(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to serialize JSON field", exception);
        }
    }

    public Map<String, Object> map(String json) {
        try {
            return mapper.readValue(json, new TypeReference<>() {});
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to read JSON object field", exception);
        }
    }

    public List<String> stringList(String json) {
        try {
            return mapper.readValue(json, new TypeReference<>() {});
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to read JSON string list field", exception);
        }
    }

    public <T> List<T> list(String json, TypeReference<List<T>> type) {
        try {
            return mapper.readValue(json, type);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to read JSON list field", exception);
        }
    }
}
