package com.poly.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poly.models.entities.Category;
import com.poly.models.entities.Product;
import com.poly.models.repositories.CategoryRepository;
import com.poly.models.repositories.ProductRepository;
import com.poly.models.requests.ProductRequest;

class ProductControllerIT extends BaseControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Long testProductId;
    private Long testCategoryId;

    @BeforeEach
    void setUp() {
        Category cat = new Category();
        cat.setName("Danh mục hoa tươi");
        cat = categoryRepository.save(cat);
        testCategoryId = cat.getId();

        Product prod = new Product();
        prod.setName("Hoa Hướng Dương");
        prod.setPrice(BigDecimal.valueOf(120_000));
        prod.setQuantity(50);
        prod.setCategory(cat);
        prod.setAvailable(true);
        prod.setDeleted(false);
        prod = productRepository.save(prod);
        testProductId = prod.getId();
    }

    // TC_PROD_IT_01: GET /product công khai -> 200 OK
    @Test
    @DisplayName("GET /product - Xem danh sách sản phẩm công khai trả về 200 OK")
    void filterProducts_public_returns200() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/product")
                .param("pageNumber", "0")
                .param("pageSize", "5"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    // TC_PROD_IT_02: GET /product/{id} tìm thấy -> 200 OK
    @Test
    @DisplayName("GET /product/{id} - Xem chi tiết sản phẩm công khai trả về 200 OK")
    void findProductById_existing_returns200() throws Exception {
        mockMvc.perform(get("/product/" + testProductId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Hoa Hướng Dương"));
    }

    // TC_PROD_IT_03: GET /product/{id} không tìm thấy -> 404 Not Found
    @Test
    @DisplayName("GET /product/{id} - Không tìm thấy sản phẩm trả về 404 Not Found")
    void findProductById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/product/999999"))
                .andExpect(status().isNotFound());
    }

    // TC_PROD_IT_04: POST /admin/product chưa đăng nhập -> 401
    @Test
    @DisplayName("POST /admin/product - Chưa đăng nhập trả về 401 Unauthorized")
    void saveProduct_unauthenticated_returns401() throws Exception {
        ProductRequest request = new ProductRequest();
        request.setName("Hoa Mới");
        request.setPrice(BigDecimal.valueOf(80_000));
        request.setQuantity(10);
        request.setCategoryId(testCategoryId);

        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", "application/json", objectMapper.writeValueAsBytes(request));

        mockMvc.perform(multipart("/admin/product").file(requestPart))
                .andExpect(status().isUnauthorized());
    }

    // TC_PROD_IT_05: POST /admin/product với USER -> 403
    @Test
    @WithMockUser(authorities = "USER")
    @DisplayName("POST /admin/product - Quyền USER bị từ chối 403 Forbidden")
    void saveProduct_asUser_returns403() throws Exception {
        ProductRequest request = new ProductRequest();
        request.setName("Hoa Mới");
        request.setPrice(BigDecimal.valueOf(80_000));
        request.setQuantity(10);
        request.setCategoryId(testCategoryId);

        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", "application/json", objectMapper.writeValueAsBytes(request));

        mockMvc.perform(multipart("/admin/product").file(requestPart))
                .andExpect(status().isForbidden());
    }

    // TC_PROD_IT_06: POST /admin/product với ADMIN hợp lệ -> 200 OK
    @Test
    @WithMockUser(authorities = "ADMIN")
    @DisplayName("POST /admin/product - Quyền ADMIN tạo sản phẩm thành công trả về 200 OK")
    void saveProduct_asAdmin_returns200() throws Exception {
        ProductRequest request = new ProductRequest();
        request.setName("Bó hoa đồng tiền");
        request.setPrice(BigDecimal.valueOf(95_000));
        request.setQuantity(30);
        request.setCategoryId(testCategoryId);

        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", "application/json", objectMapper.writeValueAsBytes(request));

        mockMvc.perform(multipart("/admin/product").file(requestPart))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Bó hoa đồng tiền"));
    }

    // TC_PROD_IT_07: POST /admin/product thiếu tên hoặc giá âm -> 400 Bad Request
    @Test
    @WithMockUser(authorities = "ADMIN")
    @DisplayName("POST /admin/product - Dữ liệu không hợp lệ trả về 400 Bad Request")
    void saveProduct_invalidData_returns400() throws Exception {
        ProductRequest request = new ProductRequest();
        request.setName("");
        request.setPrice(BigDecimal.valueOf(-100));
        request.setQuantity(-5);

        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", "application/json", objectMapper.writeValueAsBytes(request));

        mockMvc.perform(multipart("/admin/product").file(requestPart))
                .andExpect(status().isBadRequest());
    }

    // TC_PROD_IT_08: DELETE /admin/product/{id} thành công -> 200 OK
    @Test
    @WithMockUser(authorities = "ADMIN")
    @DisplayName("DELETE /admin/product/{id} - Xóa mềm sản phẩm trả về 200 OK")
    void deleteProduct_asAdmin_returns200() throws Exception {
        mockMvc.perform(delete("/admin/product/" + testProductId))
                .andExpect(status().isOk());
    }
}
