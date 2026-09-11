package com.poly.controllers;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.poly.models.requests.AiChatRequest;
import com.poly.models.responses.AiChatResponse;
import com.poly.models.services.ShopDataTools;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/ai")
public class AiController {

    private static final String SYSTEM_PROMPT = """
        You are a helpful assistant for a flower shop.
        Give concise, practical answers about flowers, bouquets, gifts, and flower care.
        For every question about this shop's products, prices, stock, categories, customisation,
        descriptions, or discounts, use the provided database tools before answering.
        Treat tool results as the only source of truth for shop data. Never invent missing data.
        If no matching database record is returned, clearly say that no matching product was found.
        Never claim that you permanently learned database data; it is retrieved live for each request.
        """;

    private final ChatClient chatClient;
    private final Executor aiTaskExecutor;


    //constructor
    public AiController(
            ChatClient.Builder chatClientBuilder,
            ShopDataTools shopDataTools,
            @Qualifier("aiTaskExecutor") Executor aiTaskExecutor) {
        this.chatClient = chatClientBuilder
            .defaultSystem(SYSTEM_PROMPT)
            .defaultTools(shopDataTools)
            .build();
        this.aiTaskExecutor = aiTaskExecutor;
    }

    @PostMapping("/chat")
    public CompletableFuture<AiChatResponse> chat(@Valid @RequestBody AiChatRequest request) {
        String message = request.getMessage().trim();
        return CompletableFuture.supplyAsync(() -> generateResponse(message), aiTaskExecutor);
    }

    private AiChatResponse generateResponse(String message) {
        String content = chatClient
            .prompt()
            .user(message)
            .call()
            .content();

        return new AiChatResponse(content == null ? "" : content);
    }
}
