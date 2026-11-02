package com.poly.models.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import com.poly.models.entities.Category;
import com.poly.models.entities.ChatMessage;
import com.poly.models.entities.Discount;
import com.poly.models.entities.Product;
import com.poly.models.mappers.ChatMessageMapper;
import com.poly.models.repositories.CategoryRepository;
import com.poly.models.repositories.ChatMessageRepository;
import com.poly.models.repositories.DiscountRepository;
import com.poly.models.repositories.ProductRepository;
import com.poly.models.requests.ChatMessageRequest;
import com.poly.models.responses.ChatMessageResponse;
import com.poly.models.services.impl.ChatMessageAsyncService;

@ExtendWith(MockitoExtension.class)
class AiChatServiceTest {

    // Mocks for ChatMessageAsyncService
    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ChatMessageMapper chatMessageMapper;

    @InjectMocks
    private ChatMessageAsyncService chatMessageAsyncService;

    // Mocks for ShopDataTools
    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private DiscountRepository discountRepository;

    @InjectMocks
    private ShopDataTools shopDataTools;

    @Test
    @DisplayName("TC_AICHAT_001: saveMessage with valid request saves message and returns response")
    void saveMessage_validRequest_returnsSavedMessageResponse() throws Exception {
        // TC_AICHAT_001
        ChatMessageRequest request = new ChatMessageRequest();
        request.setContent("Hello shop!");
        request.setSenderId(1L);
        request.setReceiverId(2L);

        ChatMessage entity = new ChatMessage();
        entity.setContent("Hello shop!");

        ChatMessage savedEntity = new ChatMessage();
        savedEntity.setId(10L);
        savedEntity.setContent("Hello shop!");

        ChatMessageResponse expectedResponse = new ChatMessageResponse();
        expectedResponse.setId("10");
        expectedResponse.setContent("Hello shop!");

        when(chatMessageMapper.toEntity(request)).thenReturn(entity);
        when(chatMessageRepository.save(entity)).thenReturn(savedEntity);
        when(chatMessageMapper.toResponse(savedEntity)).thenReturn(expectedResponse);

        CompletableFuture<ChatMessageResponse> future = chatMessageAsyncService.saveMessage(request);
        ChatMessageResponse actual = future.get();

        assertNotNull(actual);
        assertEquals("10", actual.getId());
        assertEquals("Hello shop!", actual.getContent());
        verify(chatMessageRepository).save(entity);
    }

    @Test
    @DisplayName("TC_AICHAT_002: getMessages returns list of all messages")
    void getMessages_returnsAllMessages() throws Exception {
        // TC_AICHAT_002
        ChatMessage msg = new ChatMessage();
        msg.setId(1L);
        msg.setContent("Hi");

        ChatMessageResponse resp = new ChatMessageResponse();
        resp.setId("1");
        resp.setContent("Hi");

        when(chatMessageRepository.findAll()).thenReturn(List.of(msg));
        when(chatMessageMapper.toResponseList(List.of(msg))).thenReturn(List.of(resp));

        CompletableFuture<List<ChatMessageResponse>> future = chatMessageAsyncService.getMessages();
        List<ChatMessageResponse> actual = future.get();

        assertNotNull(actual);
        assertEquals(1, actual.size());
        assertEquals("Hi", actual.get(0).getContent());
        verify(chatMessageRepository).findAll();
    }

    @Test
    @DisplayName("TC_AICHAT_003: searchCatalog with all filters returns matching products")
    void searchCatalog_withAllFilters_returnsProducts() {
        // TC_AICHAT_003
        Category cat = new Category();
        cat.setId(5L);
        cat.setName("Roses");

        Product p = new Product();
        p.setId(100L);
        p.setName("Red Rose");
        p.setDescription("Fresh red roses");
        p.setCategory(cat);
        p.setPrice(BigDecimal.valueOf(50000));
        p.setQuantity(20);
        p.setAvailable(true);
        p.setCustomised(false);
        p.setSales(5L);

        when(productRepository.searchAvailableForAi(
                eq("rose"), eq("roses"), eq(BigDecimal.valueOf(10000)), eq(BigDecimal.valueOf(100000)), eq(false), any(Pageable.class)))
            .thenReturn(List.of(p));

        List<ShopDataTools.ProductKnowledge> result = shopDataTools.searchCatalog("rose", "roses", BigDecimal.valueOf(10000), BigDecimal.valueOf(100000), false);

        assertNotNull(result);
        assertEquals(1, result.size());
        ShopDataTools.ProductKnowledge pk = result.get(0);
        assertEquals(100L, pk.id());
        assertEquals("Red Rose", pk.name());
        assertEquals(5L, pk.categoryId());
        assertEquals("Roses", pk.categoryName());
        assertTrue(pk.available());
        assertFalse(pk.customisable());
    }

    @Test
    @DisplayName("TC_AICHAT_004: searchCatalog with blank strings normalizes to null")
    void searchCatalog_withBlankKeywords_normalizesToNull() {
        // TC_AICHAT_004
        when(productRepository.searchAvailableForAi(
                eq(null), eq(null), eq(null), eq(null), eq(null), any(Pageable.class)))
            .thenReturn(Collections.emptyList());

        List<ShopDataTools.ProductKnowledge> result = shopDataTools.searchCatalog("   ", "", null, null, null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("TC_AICHAT_005: getProductById with null id returns null")
    void getProductById_nullId_returnsNull() {
        // TC_AICHAT_005
        ShopDataTools.ProductKnowledge result = shopDataTools.getProductById(null);
        assertNull(result);
    }

    @Test
    @DisplayName("TC_AICHAT_006: getProductById with found id returns ProductKnowledge")
    void getProductById_found_returnsProductKnowledge() {
        // TC_AICHAT_006
        Product p = new Product();
        p.setId(200L);
        p.setName("Tulip");
        p.setPrice(BigDecimal.valueOf(30000));
        p.setQuantity(10);
        p.setAvailable(true);

        when(productRepository.findActiveByIdForAi(200L)).thenReturn(Optional.of(p));

        ShopDataTools.ProductKnowledge result = shopDataTools.getProductById(200L);

        assertNotNull(result);
        assertEquals(200L, result.id());
        assertEquals("Tulip", result.name());
        assertNull(result.categoryId());
    }

    @Test
    @DisplayName("TC_AICHAT_007: getProductById with not found id returns null")
    void getProductById_notFound_returnsNull() {
        // TC_AICHAT_007
        when(productRepository.findActiveByIdForAi(999L)).thenReturn(Optional.empty());

        ShopDataTools.ProductKnowledge result = shopDataTools.getProductById(999L);

        assertNull(result);
    }

    @Test
    @DisplayName("TC_AICHAT_008: listCategories returns list of active categories")
    void listCategories_returnsCategories() {
        // TC_AICHAT_008
        Category cat = new Category();
        cat.setId(1L);
        cat.setName("Orchid");
        cat.setDescription("Beautiful orchids");

        when(categoryRepository.findByDeletedFalseOrderByNameAsc()).thenReturn(List.of(cat));

        List<ShopDataTools.CategoryKnowledge> result = shopDataTools.listCategories();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Orchid", result.get(0).name());
        assertEquals(1L, result.get(0).id());
    }

    @Test
    @DisplayName("TC_AICHAT_009: listActiveDiscounts returns list of active discounts")
    void listActiveDiscounts_returnsDiscounts() {
        // TC_AICHAT_009
        Discount discount = new Discount();
        discount.setCode("SUMMER20");
        discount.setPercentage(BigDecimal.valueOf(20));
        discount.setDescription("Summer discount");
        discount.setExpiredDate(LocalDateTime.now().plusDays(5));
        discount.setProducts(Collections.emptyList());

        when(discountRepository.findActiveForAi()).thenReturn(List.of(discount));

        List<ShopDataTools.DiscountKnowledge> result = shopDataTools.listActiveDiscounts();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("SUMMER20", result.get(0).code());
        assertEquals(BigDecimal.valueOf(20), result.get(0).percentage());
        assertEquals(0, result.get(0).assignedProductCount());
    }

    @Test
    @DisplayName("TC_AICHAT_010: WebSocket sendMessage with valid content trims and returns message")
    void webSocketSendMessage_validMessage_returnsTrimmedMessage() {
        // TC_AICHAT_010
        com.poly.controllers.WebSocketChatController controller = new com.poly.controllers.WebSocketChatController();
        com.poly.models.requests.WebSocketChatMessage msg = new com.poly.models.requests.WebSocketChatMessage();
        msg.setContent("   Hello WebSocket   ");
        msg.setSenderId(1L);

        com.poly.models.requests.WebSocketChatMessage result = controller.sendMessage(msg);

        assertNotNull(result);
        assertEquals("Hello WebSocket", result.getContent());
    }

    @Test
    @DisplayName("TC_AICHAT_011: WebSocket sendMessage with null request throws IllegalArgumentException")
    void webSocketSendMessage_nullRequest_throwsIllegalArgumentException() {
        // TC_AICHAT_011
        com.poly.controllers.WebSocketChatController controller = new com.poly.controllers.WebSocketChatController();
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> controller.sendMessage(null));
    }

    @Test
    @DisplayName("TC_AICHAT_012: WebSocket sendMessage with blank content throws IllegalArgumentException")
    void webSocketSendMessage_blankContent_throwsIllegalArgumentException() {
        // TC_AICHAT_012
        com.poly.controllers.WebSocketChatController controller = new com.poly.controllers.WebSocketChatController();
        com.poly.models.requests.WebSocketChatMessage msg = new com.poly.models.requests.WebSocketChatMessage();
        msg.setContent("   ");
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> controller.sendMessage(msg));
    }
}

