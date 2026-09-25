package com.poly.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.Principal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poly.config.JwtFilter;
import com.poly.config.SecurityConfig;
import com.poly.models.requests.AccountRequest;
import com.poly.models.requests.CartRequest;
import com.poly.models.requests.CommentRequest;
import com.poly.models.requests.OrderRequest;
import com.poly.models.requests.PaymentRequest;
import com.poly.models.requests.ReplyRequest;
import com.poly.models.responses.AccountResponse;
import com.poly.models.responses.CartResponse;
import com.poly.models.responses.CommentResponse;
import com.poly.models.responses.OrderResponse;
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
import com.poly.models.services.impl.FilterAsyncService;

@WebMvcTest(
    controllers = UserController.class,
    excludeAutoConfiguration = {SecurityAutoConfiguration.class},
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {SecurityConfig.class, JwtFilter.class}
    )
)
@AutoConfigureMockMvc(addFilters = false)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // BaseService dependencies
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

    // UserController dependencies
    @MockBean private FilterAsyncService filterAsyncService;
    @MockBean private OrderNotificationWebSocketController orderNotificationWebSocketController;

    private Principal mockPrincipal = () -> "testUser";

    @Test
    void testUpdateProfile() throws Exception {
        String accountJson = "{ \"username\": \"testUser\", \"email\": \"test@example.com\", \"fullname\": \"John Doe\", \"password\": \"12345\" }";
        
        AccountResponse current = new AccountResponse();
        current.setId("1");
        current.setUsername("testUser");
        current.setEmail("test@example.com");

        when(accountService.findByUsername("testUser")).thenReturn(current);
        when(accountService.save(any(AccountRequest.class))).thenReturn(current);

        MockMultipartFile accountPart = new MockMultipartFile(
                "account",
                "",
                "application/json",
                accountJson.getBytes()
        );

        mockMvc.perform(multipart("/user/account/update")
                .file(accountPart)
                .principal(mockPrincipal)
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk());
    }

    @Test
    void testSaveCart() throws Exception {
        CartRequest req = new CartRequest();
        when(cartService.save(any(CartRequest.class))).thenReturn(new CartResponse());

        mockMvc.perform(post("/user/cart")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void testDeleteCartById() throws Exception {
        mockMvc.perform(delete("/user/cart/1"))
                .andExpect(status().isOk());
    }

    @Test
    void testFindCartById() throws Exception {
        when(cartService.findById(1L)).thenReturn(new CartResponse());
        mockMvc.perform(get("/user/cart/1"))
                .andExpect(status().isOk());
    }

    @Test
    void testSaveComment() throws Exception {
        String jsonReq = "{ \"productId\": 1, \"content\": \"Good product!\" }";
        when(commentService.save(any(CommentRequest.class))).thenReturn(new CommentResponse());

        mockMvc.perform(post("/user/comment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonReq))
                .andExpect(status().isOk());
    }

    @Test
    void testDeleteCommentById() throws Exception {
        mockMvc.perform(delete("/user/comment/1"))
                .andExpect(status().isOk());
    }

    @Test
    void testFindCommentById() throws Exception {
        when(commentService.findById(1L)).thenReturn(new CommentResponse());
        mockMvc.perform(get("/user/comment/1"))
                .andExpect(status().isOk());
    }

    @Test
    void testFindMyOrderById() throws Exception {
        when(orderService.findMineById("testUser", 1L)).thenReturn(new OrderResponse());

        mockMvc.perform(get("/user/order/mine/1")
                .principal(mockPrincipal))
                .andExpect(status().isOk());
    }

    @Test
    void testSaveOrder() throws Exception {
        String jsonReq = "{ \"accountId\": 1, \"fullname\": \"John\", \"phone\": \"0123456789\", \"address\": \"123 St\", \"paymentMethod\": \"COD\", \"orderDetailRequests\": [{\"productId\": 1, \"quantity\": 1}] }";
        OrderResponse res = new OrderResponse();
        when(orderService.save(any(OrderRequest.class))).thenReturn(res);

        mockMvc.perform(post("/user/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonReq))
                .andExpect(status().isOk());
    }

    @Test
    void testDeleteOrderById() throws Exception {
        mockMvc.perform(delete("/user/order/1"))
                .andExpect(status().isOk());
    }

    @Test
    void testFindOrderById() throws Exception {
        when(orderService.findById(1L)).thenReturn(new OrderResponse());
        mockMvc.perform(get("/user/order/1"))
                .andExpect(status().isOk());
    }

    @Test
    void testSavePayment() throws Exception {
        String jsonReq = "{ \"orderId\": 1, \"amount\": 100.0, \"paymentMethod\": \"COD\" }";
        
        when(orderService.findById(1L)).thenReturn(new OrderResponse());

        mockMvc.perform(post("/user/payment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonReq))
                .andExpect(status().isOk());
    }

    @Test
    void testSaveReply() throws Exception {
        String jsonReq = "{ \"commentId\": 1, \"content\": \"Thanks!\" }";
        when(replyService.save(any(ReplyRequest.class))).thenReturn(new ReplyResponse());

        mockMvc.perform(post("/user/reply")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonReq))
                .andExpect(status().isOk());
    }

    @Test
    void testDeleteReplyById() throws Exception {
        mockMvc.perform(delete("/user/reply/1"))
                .andExpect(status().isOk());
    }

    @Test
    void testFindReplyById() throws Exception {
        when(replyService.findById(1L)).thenReturn(new ReplyResponse());
        mockMvc.perform(get("/user/reply/1"))
                .andExpect(status().isOk());
    }
}
