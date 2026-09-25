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
import java.util.List;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poly.config.JwtFilter;
import com.poly.config.SecurityConfig;
import com.poly.models.requests.AccountRequest;
import com.poly.models.requests.AdminOrderUpdateRequest;
import com.poly.models.requests.CategoryRequest;
import com.poly.models.requests.CommentRequest;
import com.poly.models.requests.DiscountRequest;
import com.poly.models.requests.ProductImageRequest;
import com.poly.models.requests.ProductRequest;
import com.poly.models.responses.AccountResponse;
import com.poly.models.responses.CategoryResponse;
import com.poly.models.responses.CommentResponse;
import com.poly.models.responses.DiscountResponse;
import com.poly.models.responses.OrderResponse;
import com.poly.models.responses.ProductImageResponse;
import com.poly.models.responses.ProductResponse;
import com.poly.models.services.AccountService;
import com.poly.models.services.CartService;
import com.poly.models.services.CategoryService;
import com.poly.models.services.CommentService;
import com.poly.models.services.DiscountService;
import com.poly.models.services.ImageService;
import com.poly.models.services.NotificationService;
import com.poly.models.services.OrderService;
import com.poly.models.services.PaymentService;
import com.poly.models.services.ProductImageService;
import com.poly.models.services.ProductService;
import com.poly.models.services.QrService;
import com.poly.models.services.ReplyService;
import com.poly.models.services.impl.FilterAsyncService;

@WebMvcTest(
    controllers = AdminController.class,
    excludeAutoConfiguration = {SecurityAutoConfiguration.class},
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {SecurityConfig.class, JwtFilter.class}
    )
)
@AutoConfigureMockMvc(addFilters = false)
public class AdminControllerTest {

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

    // AdminController dependencies
    @MockBean private FilterAsyncService filterAsyncService;
    @MockBean private OrderNotificationWebSocketController orderNotificationWebSocketController;
    @MockBean private NotificationService notificationService;

    @Test
    void testUpdateOrder() throws Exception {
        String jsonReq = "{ \"fullname\": \"John\", \"phone\": \"0123456789\", \"address\": \"123 St\", \"shippingFee\": 10.0, \"status\": \"PENDING\" }";
        when(orderService.updateAdminInfo(eq(1L), any(AdminOrderUpdateRequest.class))).thenReturn(new OrderResponse());

        mockMvc.perform(post("/admin/order/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonReq))
                .andExpect(status().isOk());
    }

    @Test
    void testFindNotifications() throws Exception {
        mockMvc.perform(get("/admin/notification"))
                .andExpect(status().isOk());
    }

    @Test
    void testMarkAllNotificationsRead() throws Exception {
        mockMvc.perform(post("/admin/notification/read-all"))
                .andExpect(status().isOk());
    }

    @Test
    void testClearNotifications() throws Exception {
        mockMvc.perform(delete("/admin/notification"))
                .andExpect(status().isOk());
    }

    @Test
    void testSaveAccount() throws Exception {
        String accountJson = "{ \"username\": \"adminTest\", \"email\": \"admin@test.com\", \"fullname\": \"Admin Name\", \"password\": \"12345\" }";
        when(accountService.save(any(AccountRequest.class))).thenReturn(new AccountResponse());

        MockMultipartFile accountPart = new MockMultipartFile(
                "account",
                "",
                "application/json",
                accountJson.getBytes()
        );

        mockMvc.perform(multipart("/admin/account")
                .file(accountPart)
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk());
    }

    @Test
    void testDeleteAccount() throws Exception {
        mockMvc.perform(delete("/admin/account/1"))
                .andExpect(status().isOk());
    }

    @Test
    void testFindAccountById() throws Exception {
        when(accountService.findById(1L)).thenReturn(new AccountResponse());
        mockMvc.perform(get("/admin/account/1"))
                .andExpect(status().isOk());
    }

    @Test
    void testFindAccountByUsername() throws Exception {
        when(accountService.findByUsername("test")).thenReturn(new AccountResponse());
        mockMvc.perform(get("/admin/account/by-username/test"))
                .andExpect(status().isOk());
    }

    @Test
    void testFindAccountByEmail() throws Exception {
        when(accountService.findByEmail("test@test.com")).thenReturn(new AccountResponse());
        mockMvc.perform(get("/admin/account/by-email/test@test.com"))
                .andExpect(status().isOk());
    }

    @Test
    void testFilterAndPaginateAccounts() throws Exception {
        mockMvc.perform(get("/admin/account"))
                .andExpect(status().isOk());
    }

    @Test
    void testSaveCategory() throws Exception {
        String jsonReq = "{ \"name\": \"New Category\" }";
        when(categoryService.save(any(CategoryRequest.class))).thenReturn(new CategoryResponse());

        mockMvc.perform(post("/admin/category")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonReq))
                .andExpect(status().isOk());
    }

    @Test
    void testDeleteCategoryById() throws Exception {
        mockMvc.perform(delete("/admin/category/1"))
                .andExpect(status().isOk());
    }

    @Test
    void testFindCategoryById() throws Exception {
        when(categoryService.findById(1L)).thenReturn(new CategoryResponse());
        mockMvc.perform(get("/admin/category/1"))
                .andExpect(status().isOk());
    }

    @Test
    void testFilterAndPaginateCategories() throws Exception {
        mockMvc.perform(get("/admin/category"))
                .andExpect(status().isOk());
    }

    @Test
    void testSaveDiscount() throws Exception {
        String jsonReq = "{ \"code\": \"DISCOUNT10\", \"percentage\": 10, \"active\": true, \"quantity\": 100 }";
        when(discountService.save(any(DiscountRequest.class))).thenReturn(new DiscountResponse());

        mockMvc.perform(post("/admin/discount")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonReq))
                .andExpect(status().isOk());
    }

    @Test
    void testDeleteDiscountById() throws Exception {
        mockMvc.perform(delete("/admin/discount/1"))
                .andExpect(status().isOk());
    }

    @Test
    void testFindDiscountById() throws Exception {
        when(discountService.findById(1L)).thenReturn(new DiscountResponse());
        mockMvc.perform(get("/admin/discount/1"))
                .andExpect(status().isOk());
    }

    @Test
    void testFilterAndPaginateDiscounts() throws Exception {
        mockMvc.perform(get("/admin/discount"))
                .andExpect(status().isOk());
    }

    @Test
    void testUpdateDiscountProducts() throws Exception {
        String jsonReq = "{ \"productIds\": [1, 2, 3] }";
        when(discountService.updateProducts(eq(1L), any(List.class))).thenReturn(new DiscountResponse());

        mockMvc.perform(post("/admin/discount/1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonReq))
                .andExpect(status().isOk());
    }

    @Test
    void testSaveProduct() throws Exception {
        String jsonReq = "{ \"name\": \"Product\", \"price\": 100.0, \"quantity\": 10, \"categoryId\": 1 }";
        when(productService.save(any(ProductRequest.class))).thenReturn(new ProductResponse());

        MockMultipartFile requestPart = new MockMultipartFile(
                "request",
                "",
                "application/json",
                jsonReq.getBytes()
        );

        mockMvc.perform(multipart("/admin/product")
                .file(requestPart)
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk());
    }

    @Test
    void testDeleteProductById() throws Exception {
        mockMvc.perform(delete("/admin/product/1"))
                .andExpect(status().isOk());
    }

    @Test
    void testAddDiscountToProduct() throws Exception {
        String jsonReq = "{ \"productId\": 1, \"discountIds\": [1, 2] }";
        when(discountService.addDiscounts(eq(1L), any(List.class))).thenReturn(new ProductResponse());

        mockMvc.perform(post("/admin/product/add/discount")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonReq))
                .andExpect(status().isOk());
    }

    @Test
    void testDeleteImageById() throws Exception {
        mockMvc.perform(post("/admin/image/delete/1"))
                .andExpect(status().isOk());
    }

    @Test
    void testUploadImage() throws Exception {
        MockMultipartFile filePart = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test content".getBytes()
        );

        when(imageService.upload(any())).thenReturn("test.jpg");
        when(productImageService.save(any())).thenReturn(new ProductImageResponse());

        mockMvc.perform(multipart("/admin/image/upload")
                .file(filePart)
                .param("productId", "1")
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk());
    }

    @Test
    void testFilterAndPaginateProductImages() throws Exception {
        mockMvc.perform(get("/admin/image"))
                .andExpect(status().isOk());
    }

    @Test
    void testSaveComment() throws Exception {
        String jsonReq = "{ \"productId\": 1, \"content\": \"Test comment\" }";
        when(commentService.save(any(CommentRequest.class))).thenReturn(new CommentResponse());

        mockMvc.perform(post("/admin/comment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonReq))
                .andExpect(status().isOk());
    }

    @Test
    void testDeleteCommentById() throws Exception {
        mockMvc.perform(delete("/admin/comment/1"))
                .andExpect(status().isOk());
    }
}
