package com.poly.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poly.config.JwtFilter;
import com.poly.config.SecurityConfig;
import com.poly.models.requests.ChatMessageRequest;
import com.poly.models.responses.ChatMessageResponse;
import com.poly.models.services.impl.ChatMessageAsyncService;

@WebMvcTest(
    controllers = ChatMessageController.class,
    excludeAutoConfiguration = {SecurityAutoConfiguration.class},
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {SecurityConfig.class, JwtFilter.class}
    )
)
@AutoConfigureMockMvc(addFilters = false)
public class ChatMessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean private ChatMessageAsyncService chatMessageAsyncService;

    @Test
    void testSaveMessage() throws Exception {
        String jsonReq = "{ \"content\": \"Hello\", \"senderId\": 1 }";
        when(chatMessageAsyncService.saveMessage(any(ChatMessageRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(new ChatMessageResponse()));

        mockMvc.perform(post("/chat/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonReq))
                .andExpect(status().isCreated());
    }

    @Test
    void testGetMessages() throws Exception {
        when(chatMessageAsyncService.getMessages())
            .thenReturn(CompletableFuture.completedFuture(new ArrayList<>()));

        mockMvc.perform(get("/chat/messages"))
                .andExpect(status().isOk());
    }
}
