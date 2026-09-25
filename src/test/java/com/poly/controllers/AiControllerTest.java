package com.poly.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.Executor;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.poly.config.JwtFilter;
import com.poly.config.SecurityConfig;
import com.poly.models.services.ShopDataTools;

@WebMvcTest(
    controllers = AiController.class,
    excludeAutoConfiguration = {SecurityAutoConfiguration.class},
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {SecurityConfig.class, JwtFilter.class}
    )
)
@AutoConfigureMockMvc(addFilters = false)
public class AiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @TestConfiguration
    static class AiControllerTestConfig {
        
        @Bean
        public ChatClient.Builder chatClientBuilder() {
            ChatClient.Builder builder = org.mockito.Mockito.mock(ChatClient.Builder.class);
            ChatClient chatClient = org.mockito.Mockito.mock(ChatClient.class);
            when(builder.defaultSystem(any(String.class))).thenReturn(builder);
            when(builder.defaultTools(any())).thenReturn(builder);
            when(builder.build()).thenReturn(chatClient);
            
            ChatClient.ChatClientRequestSpec spec = org.mockito.Mockito.mock(ChatClient.ChatClientRequestSpec.class);
            ChatClient.CallResponseSpec callSpec = org.mockito.Mockito.mock(ChatClient.CallResponseSpec.class);
            
            when(chatClient.prompt()).thenReturn(spec);
            when(spec.user(any(String.class))).thenReturn(spec);
            when(spec.call()).thenReturn(callSpec);
            when(callSpec.content()).thenReturn("Mocked AI response");
            
            return builder;
        }

        @Bean(name = "aiTaskExecutor")
        public Executor aiTaskExecutor() {
            // Run synchronously for test purposes
            return Runnable::run;
        }
        
        @Bean
        public ShopDataTools shopDataTools() {
            return org.mockito.Mockito.mock(ShopDataTools.class);
        }
    }

    @Test
    void testChat() throws Exception {
        String jsonReq = "{ \"message\": \"Hello AI\" }";

        MvcResult result = mockMvc.perform(post("/ai/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonReq))
                .andExpect(status().isOk())
                .andReturn();
    }
}
