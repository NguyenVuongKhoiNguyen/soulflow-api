package com.poly.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Principal;

public class UserControllerIT extends BaseIT {

    @Autowired
    private MockMvc mockMvc;

    private Principal mockPrincipal = () -> "testUser";

    @Test
    void testFindMyOrderById_NotFound() throws Exception {
        // Without setting up order data in DB, it should either return 404 or empty response.
        // Assuming 404 Not Found since it's an ID lookup
        mockMvc.perform(get("/user/order/mine/999")
                .principal(mockPrincipal))
                .andExpect(status().isNotFound());
    }
}
