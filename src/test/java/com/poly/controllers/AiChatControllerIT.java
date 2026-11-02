package com.poly.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import com.poly.models.entities.Account;
import com.poly.models.repositories.AccountRepository;
import com.poly.models.repositories.ChatMessageRepository;
import com.poly.models.requests.AiChatRequest;
import com.poly.models.requests.ChatMessageRequest;

class AiChatControllerIT extends BaseControllerIT {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    private Account senderAccount;

    @BeforeEach
    void setUp() {
        chatMessageRepository.deleteAll();

        senderAccount = accountRepository.findByUsername("chatsender").orElseGet(() -> {
            Account acc = new Account();
            acc.setUsername("chatsender");
            acc.setPassword("encoded123");
            acc.setEmail("chatsender@example.com");
            acc.setFullname("Chat Sender");
            acc.setPhone("0909000111");
            acc.setDeleted(false);
            acc.setDisabled(false);
            return accountRepository.save(acc);
        });
    }

    @Test
    @DisplayName("TC_AICHAT_IT_001: POST /chat/messages with valid request creates message and returns 201")
    void saveMessage_validRequest_returns201() throws Exception {
        // TC_AICHAT_IT_001
        ChatMessageRequest req = new ChatMessageRequest();
        req.setContent("Hello, I need assistance with flowers.");
        req.setSenderId(senderAccount.getId());

        MvcResult mvcResult = mockMvc.perform(post("/chat/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(request().asyncStarted())
            .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.content").value("Hello, I need assistance with flowers."))
            .andExpect(jsonPath("$.senderUsername").value("chatsender"));
    }

    @Test
    @DisplayName("TC_AICHAT_IT_002: POST /chat/messages with blank content returns 400")
    void saveMessage_blankContent_returns400() throws Exception {
        // TC_AICHAT_IT_002
        ChatMessageRequest req = new ChatMessageRequest();
        req.setContent("   ");
        req.setSenderId(senderAccount.getId());

        mockMvc.perform(post("/chat/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TC_AICHAT_IT_003: POST /chat/messages with missing senderId returns 400")
    void saveMessage_missingSenderId_returns400() throws Exception {
        // TC_AICHAT_IT_003
        ChatMessageRequest req = new ChatMessageRequest();
        req.setContent("Hello there");
        req.setSenderId(null);

        mockMvc.perform(post("/chat/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TC_AICHAT_IT_004: POST /chat/messages with non-existent senderId returns 404")
    void saveMessage_nonExistentSender_returns404() throws Exception {
        // TC_AICHAT_IT_004
        ChatMessageRequest req = new ChatMessageRequest();
        req.setContent("Hello from nobody");
        req.setSenderId(99999L);

        MvcResult mvcResult = mockMvc.perform(post("/chat/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(request().asyncStarted())
            .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("TC_AICHAT_IT_005: GET /chat/messages returns 200 with list of messages")
    void getMessages_public_returns200() throws Exception {
        // TC_AICHAT_IT_005
        MvcResult mvcResult = mockMvc.perform(get("/chat/messages"))
            .andExpect(request().asyncStarted())
            .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("TC_AICHAT_IT_006: POST /ai/chat with blank message returns 400")
    void chat_blankMessage_returns400() throws Exception {
        // TC_AICHAT_IT_006
        AiChatRequest req = new AiChatRequest();
        req.setMessage("");

        mockMvc.perform(post("/ai/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TC_AICHAT_IT_007: POST /ai/chat with message exceeding 4000 characters returns 400")
    void chat_tooLongMessage_returns400() throws Exception {
        // TC_AICHAT_IT_007
        AiChatRequest req = new AiChatRequest();
        req.setMessage("A".repeat(4001));

        mockMvc.perform(post("/ai/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest());
    }
}
