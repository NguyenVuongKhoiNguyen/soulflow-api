package com.poly.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
import com.poly.models.enums.SortOrder;
import com.poly.models.requests.AuthRequest;
import com.poly.models.requests.OrderRequest;
import com.poly.models.requests.SepayWebhookRequest;
import com.poly.models.responses.AccountResponse;
import com.poly.models.responses.AuthResponse;
import com.poly.models.responses.CategoryResponse;
import com.poly.models.responses.CommentResponse;
import com.poly.models.responses.OrderResponse;
import com.poly.models.responses.PageResponse;
import com.poly.models.responses.ProductResponse;
import com.poly.models.responses.ReplyResponse;
import com.poly.models.services.AccountService;
import com.poly.models.services.CartService;
import com.poly.models.services.CategoryService;
import com.poly.models.services.CommentService;
import com.poly.models.services.DiscountService;
import com.poly.models.services.ImageService;
import com.poly.models.services.OrderService;
import com.poly.models.services.PaymentService;
import com.poly.models.services.ProductImageService;
import com.poly.models.services.ProductService;
import com.poly.models.services.QrService;
import com.poly.models.services.ReplyService;
import com.poly.models.services.impl.AccountServiceImpl.GoogleTokenDTO;
import com.poly.models.services.impl.FilterAsyncService;
import com.poly.models.services.impl.RefreshTokenService;
import com.poly.models.services.impl.SepayWebhookService;

@WebMvcTest(
    controllers = NonUserController.class,
    excludeAutoConfiguration = {SecurityAutoConfiguration.class},
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {SecurityConfig.class, JwtFilter.class}
    )
)
@AutoConfigureMockMvc(addFilters = false)
public class NonUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Dependencies in BaseService
    @MockBean public ImageService imageService;
    @MockBean public AccountService accountService;
    @MockBean public CategoryService categoryService;
    @MockBean public ProductService productService;
    @MockBean public CartService cartService;
    @MockBean public CommentService commentService;
    @MockBean public OrderService orderService;
    @MockBean public ReplyService replyService;
    @MockBean public ProductImageService productImageService;
    @MockBean public DiscountService discountService;
    @MockBean public PaymentService paymentService;
    @MockBean public QrService qrService;

    // Dependencies in NonUserController
    @MockBean private FilterAsyncService filterAsyncService;
    @MockBean private RefreshTokenService refreshTokens;
    @MockBean private OrderNotificationWebSocketController orderNotificationWebSocketController;
    @MockBean private SepayWebhookService sepayWebhookService;

    @Test
    void testCheckAdminAccess() throws Exception {
        mockMvc.perform(get("/admin/auth/check"))
                .andExpect(status().isNoContent());
    }

    @Test
    void testLogin() throws Exception {
        AuthRequest req = new AuthRequest();
        req.setUsername("admin");
        req.setPassword("12345");

        AuthResponse res = new AuthResponse();
        res.setToken("dummy_token");
        res.setRefreshToken("dummy_refresh");
        res.setMaxAge(3600);
        res.setRefreshMaxAge(7200);
        res.setAccountResponse(new AccountResponse());
        
        when(accountService.login(any(AuthRequest.class))).thenReturn(res);

        mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void testGoogleLogin() throws Exception {
        GoogleTokenDTO token = new GoogleTokenDTO();
        AuthResponse res = new AuthResponse();
        res.setToken("dummy_token");
        res.setRefreshToken("dummy_refresh");
        res.setMaxAge(3600);
        res.setRefreshMaxAge(7200);
        res.setAccountResponse(new AccountResponse());

        when(accountService.loginWithGoogle(any(GoogleTokenDTO.class))).thenReturn(res);

        mockMvc.perform(post("/google/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(token)))
                .andExpect(status().isOk());
    }

    @Test
    void testLogout() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isNoContent());
    }

    @Test
    void testRefresh() throws Exception {
        AuthResponse res = new AuthResponse();
        res.setToken("dummy_token");
        res.setRefreshToken("dummy_refresh");
        res.setMaxAge(3600);
        res.setRefreshMaxAge(7200);
        res.setAccountResponse(new AccountResponse());

        when(accountService.refresh(any())).thenReturn(res);

        mockMvc.perform(post("/auth/refresh"))
                .andExpect(status().isOk());
    }

    @Test
    void testFindAllCategories() throws Exception {
        when(categoryService.findAll()).thenReturn(List.of(new CategoryResponse()));

        mockMvc.perform(get("/category/list"))
                .andExpect(status().isOk());
    }

    @Test
    void testFindProductById() throws Exception {
        ProductResponse prod = new ProductResponse();
        when(productService.findById(1L)).thenReturn(prod);

        mockMvc.perform(get("/product/1"))
                .andExpect(status().isOk());
    }

}
