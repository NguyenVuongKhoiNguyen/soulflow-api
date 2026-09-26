package com.poly.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

public class NonUserControllerIT extends BaseIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testFindAllProducts() throws Exception {
        mockMvc.perform(get("/product"))
                .andExpect(status().isOk());
    }
}

