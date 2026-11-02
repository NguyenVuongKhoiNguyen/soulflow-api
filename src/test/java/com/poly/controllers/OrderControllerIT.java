package com.poly.controllers;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poly.models.entities.Account;
import com.poly.models.entities.Category;
import com.poly.models.entities.Order;
import com.poly.models.entities.Product;
import com.poly.models.entities.Store;
import com.poly.models.enums.OrderStatus;
import com.poly.models.enums.PaymentMethod;
import com.poly.models.repositories.AccountRepository;
import com.poly.models.repositories.CategoryRepository;
import com.poly.models.repositories.OrderRepository;
import com.poly.models.repositories.ProductRepository;
import com.poly.models.repositories.StoreRepository;
import com.poly.models.requests.AdminOrderUpdateRequest;
import com.poly.models.requests.OrderDetailRequest;
import com.poly.models.requests.OrderRequest;

class OrderControllerIT extends BaseControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.poly.models.services.ShippingFeeService shippingFeeService;

    private Long testOrderId;
    private Long testProductId;
    private Long testStoreId;
    private Long testAccountId;

    @BeforeEach
    void setUp() {
        when(shippingFeeService.calculateFee(anyString(), anyString())).thenReturn(BigDecimal.valueOf(15_000));

        Account acc = accountRepository.findByUsername("order_user").orElse(null);
        if (acc == null) {
            acc = new Account();
            acc.setUsername("order_user");
            acc.setEmail("order_user@gmail.com");
            acc.setFullname("Order User");
            acc.setDeleted(false);
            acc.setDisabled(false);
            acc = accountRepository.save(acc);
        }
        testAccountId = acc.getId();

        Store store = new Store();
        store.setName("Flower Shop Main");
        store.setAddress("123 Duong Hoa");
        store = storeRepository.save(store);
        testStoreId = store.getId();

        Category cat = new Category();
        cat.setName("Category Order");
        cat = categoryRepository.save(cat);

        Product p = new Product();
        p.setName("Hoa Mung Tot Nghiep");
        p.setPrice(BigDecimal.valueOf(250_000));
        p.setQuantity(100);
        p.setCategory(cat);
        p.setAvailable(true);
        p.setDeleted(false);
        p = productRepository.save(p);
        testProductId = p.getId();

        Order ord = new Order();
        ord.setFullname("Nguoi Nhan");
        ord.setPhone("0912345678");
        ord.setAddress("789 Pho Hue");
        ord.setStatus(OrderStatus.PENDING);
        ord.setAccount(acc);
        ord.setStore(store);
        ord.setDeleted(false);
        ord = orderRepository.save(ord);
        testOrderId = ord.getId();
    }

    // TC_ORD_IT_01: GET /user/order/mine khi chưa đăng nhập -> 401
    @Test
    @DisplayName("GET /user/order/mine - Chưa đăng nhập trả về 401 Unauthorized")
    void findMine_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/user/order/mine"))
                .andExpect(status().isUnauthorized());
    }

    // TC_ORD_IT_02: GET /user/order/mine khi đăng nhập USER -> 200 OK
    @Test
    @WithMockUser(username = "order_user", authorities = "USER")
    @DisplayName("GET /user/order/mine - Đăng nhập USER trả về 200 OK với danh sách đơn của mình")
    void findMine_asUser_returns200() throws Exception {
        org.springframework.test.web.servlet.MvcResult mvcResult = mockMvc.perform(get("/user/order/mine"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    // TC_ORD_IT_03: POST /user/order với dữ liệu để trống/thiếu items -> 400 Bad Request
    @Test
    @WithMockUser(username = "order_user", authorities = "USER")
    @DisplayName("POST /user/order - Thiếu danh sách sản phẩm hoặc thông tin trả về 400 Bad Request")
    void saveOrder_invalidData_returns400() throws Exception {
        OrderRequest request = new OrderRequest();
        request.setFullname("");
        request.setOrderDetailRequests(List.of());

        mockMvc.perform(post("/user/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // TC_ORD_IT_04: POST /order tạo đơn hàng công khai thành công -> 200 OK
    @Test
    @DisplayName("POST /order - Khách vãng lai tạo đơn hàng trả về 200 OK")
    void saveOrder_public_returns200() throws Exception {
        OrderDetailRequest item = new OrderDetailRequest();
        item.setProductId(testProductId);
        item.setQuantity(2);

        OrderRequest request = new OrderRequest();
        request.setFullname("Khach Mua Hoa");
        request.setPhone("0987654321");
        request.setAddress("456 Nguyen Trai, Ha Noi");
        request.setPaymentMethod(PaymentMethod.COD);
        request.setStoreId(testStoreId);
        request.setOrderDetailRequests(List.of(item));

        mockMvc.perform(post("/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullname").value("Khach Mua Hoa"));
    }

    // TC_ORD_IT_05: GET /user/order/{id} tìm thấy -> 200 OK
    @Test
    @WithMockUser(username = "order_user", authorities = "USER")
    @DisplayName("GET /user/order/{id} - Tìm thấy đơn hàng trả về 200 OK")
    void findOrderById_existing_returns200() throws Exception {
        mockMvc.perform(get("/user/order/" + testOrderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullname").value("Nguoi Nhan"));
    }

    // TC_ORD_IT_06: GET /user/order/{id} không tìm thấy -> 404 Not Found
    @Test
    @WithMockUser(username = "order_user", authorities = "USER")
    @DisplayName("GET /user/order/{id} - Không tìm thấy đơn hàng trả về 404 Not Found")
    void findOrderById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/user/order/999999"))
                .andExpect(status().isNotFound());
    }

    // TC_ORD_IT_07: POST /admin/order/{id} với quyền USER -> 403 Forbidden
    @Test
    @WithMockUser(username = "order_user", authorities = "USER")
    @DisplayName("POST /admin/order/{id} - Quyền USER bị từ chối 403 Forbidden")
    void updateAdminOrder_asUser_returns403() throws Exception {
        AdminOrderUpdateRequest req = new AdminOrderUpdateRequest();
        req.setFullname("Updated");
        req.setPhone("0912345678");
        req.setAddress("Addr");
        req.setStatus(OrderStatus.DELIVERED);

        mockMvc.perform(post("/admin/order/" + testOrderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // TC_ORD_IT_08: POST /admin/order/{id} cố mở lại đơn CANCELLED -> 409 CONFLICT
    @Test
    @WithMockUser(authorities = "ADMIN")
    @DisplayName("POST /admin/order/{id} - Cố mở lại đơn đã CANCELLED trả về 409 Conflict")
    void updateAdminOrder_reopenCancelled_returns409() throws Exception {
        Order ord = orderRepository.findById(testOrderId).orElseThrow();
        ord.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(ord);

        AdminOrderUpdateRequest req = new AdminOrderUpdateRequest();
        req.setFullname("Updated");
        req.setPhone("0912345678");
        req.setAddress("Addr");
        req.setStatus(OrderStatus.PROCESSING);

        mockMvc.perform(post("/admin/order/" + testOrderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    // TC_ORD_IT_09: DELETE /user/order/{id} thành công -> 200 OK
    @Test
    @WithMockUser(username = "order_user", authorities = "USER")
    @DisplayName("DELETE /user/order/{id} - Xóa mềm đơn hàng trả về 200 OK")
    void deleteOrder_asUser_returns200() throws Exception {
        mockMvc.perform(delete("/user/order/" + testOrderId))
                .andExpect(status().isOk());
    }
}
