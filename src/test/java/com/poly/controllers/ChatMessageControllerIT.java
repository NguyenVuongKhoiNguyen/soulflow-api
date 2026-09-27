package com.poly.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

public class ChatMessageControllerIT extends BaseIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testGetChatMessages_NotFound() throws Exception {
        mockMvc.perform(get("/chat/messages"))
                .andExpect(status().isOk());
    }
}

