package com.mcp.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ApiControllerTests {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void listsDemoTools() throws Exception {
        mockMvc.perform(get("/api/tools"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(6)))
                .andExpect(jsonPath("$[?(@.id == 'crm.customer.search')]").exists());
    }

    @Test
    void invokesMediumRiskToolDirectly() throws Exception {
        mockMvc.perform(post("/api/tools/crm.customer.search/invoke")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "environment": "production",
                                  "requester": "admin",
                                  "parameters": {
                                    "customer_id": "CUST-202405-000123",
                                    "region": "APAC",
                                    "limit": 20
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.response.demo").value(true));
    }

    @Test
    void sendsHighRiskReadonlyDbToolToReview() throws Exception {
        mockMvc.perform(post("/api/tools/db.query.readonly/invoke")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "environment": "production",
                                  "requester": "admin",
                                  "parameters": {
                                    "sql": "select * from demo_customers limit 20"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.reviewId").exists());
    }

    @Test
    void blocksDangerousSqlEvenForReadonlyTool() throws Exception {
        mockMvc.perform(post("/api/tools/db.query.readonly/invoke")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "environment": "production",
                                  "requester": "admin",
                                  "parameters": {
                                    "sql": "delete from customers"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"))
                .andExpect(jsonPath("$.response.blocked").value(true));
    }
}
