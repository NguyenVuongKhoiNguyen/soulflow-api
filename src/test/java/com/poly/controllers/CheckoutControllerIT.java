package com.poly.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

public class CheckoutControllerIT extends BaseIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testCheckoutEndpoint_NotFound() throws Exception {
        // Just checking standard endpoint mapping
        mockMvc.perform(get("/checkout/orders/999999"))
                .andExpect(status().isNotFound());
    }
}

