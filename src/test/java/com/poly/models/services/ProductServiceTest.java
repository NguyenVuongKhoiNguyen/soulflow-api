package com.poly.models.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.poly.models.entities.Product;
import com.poly.models.enums.SortOrder;
import com.poly.models.mappers.ProductMapper;
import com.poly.models.repositories.ProductRepository;
import com.poly.models.requests.ProductRequest;
import com.poly.models.responses.PageResponse;
import com.poly.models.responses.ProductResponse;
import com.poly.models.services.impl.ProductServiceImpl;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepo;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product sampleProduct;
    private ProductRequest sampleRequest;
    private ProductResponse sampleResponse;

    @BeforeEach
    void setUp() {
        sampleProduct = new Product();
        sampleProduct.setId(1L);
        sampleProduct.setName("Hoa Hồng Đỏ");
        sampleProduct.setPrice(BigDecimal.valueOf(150_000));
        sampleProduct.setQuantity(20);
        sampleProduct.setDeleted(false);

        sampleRequest = new ProductRequest();
        sampleRequest.setName("Hoa Hồng Đỏ");
        sampleRequest.setPrice(BigDecimal.valueOf(150_000));
        sampleRequest.setQuantity(20);
        sampleRequest.setCategoryId(1L);

        sampleResponse = new ProductResponse();
        sampleResponse.setId("1");
        sampleResponse.setName("Hoa Hồng Đỏ");
        sampleResponse.setPrice("150000");
        sampleResponse.setQuantity("20");
    }

    // TC_PROD_01: save sản phẩm thành công
    @Test
    @DisplayName("save - lưu sản phẩm thành công trả về ProductResponse")
    void save_validRequest_returnsProductResponse() {
        when(productMapper.toEntity(sampleRequest)).thenReturn(sampleProduct);
        when(productRepo.save(sampleProduct)).thenReturn(sampleProduct);
        when(productMapper.toBasicResponse(sampleProduct)).thenReturn(sampleResponse);

        ProductResponse result = productService.save(sampleRequest);

        assertNotNull(result);
        assertEquals("1", result.getId());
        assertEquals("Hoa Hồng Đỏ", result.getName());
        verify(productRepo).save(sampleProduct);
    }

    // TC_PROD_02: softDeleteById ném lỗi khi id null
    @Test
    @DisplayName("softDeleteById - ném IllegalArgumentException khi id null")
    void softDeleteById_nullId_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> productService.softDeleteById(null));
    }

    // TC_PROD_03: softDeleteById ném EntityNotFoundException khi không tìm thấy
    @Test
    @DisplayName("softDeleteById - không tìm thấy id ném EntityNotFoundException")
    void softDeleteById_notFound_throwsException() {
        when(productRepo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> productService.softDeleteById(999L));
    }

    // TC_PROD_04: softDeleteById thành công đánh dấu deleted = true
    @Test
    @DisplayName("softDeleteById - thành công cập nhật deleted = true")
    void softDeleteById_success() {
        when(productRepo.findById(1L)).thenReturn(Optional.of(sampleProduct));

        productService.softDeleteById(1L);

        assertTrue(sampleProduct.getDeleted());
        verify(productRepo).save(sampleProduct);
    }

    // TC_PROD_05: findById ném lỗi khi null hoặc không tìm thấy
    @Test
    @DisplayName("findById - id null ném IllegalArgumentException, không tìm thấy ném UsernameNotFoundException")
    void findById_exceptions() {
        assertThrows(IllegalArgumentException.class, () -> productService.findById(null));

        when(productRepo.findById(999L)).thenReturn(Optional.empty());
        assertThrows(UsernameNotFoundException.class, () -> productService.findById(999L));
    }

    // TC_PROD_06: findById thành công
    @Test
    @DisplayName("findById - tìm thấy trả về ProductResponse")
    void findById_success() {
        when(productRepo.findById(1L)).thenReturn(Optional.of(sampleProduct));
        when(productMapper.toBasicResponse(sampleProduct)).thenReturn(sampleResponse);

        ProductResponse result = productService.findById(1L);

        assertNotNull(result);
        assertEquals("1", result.getId());
        assertEquals("Hoa Hồng Đỏ", result.getName());
    }

    // TC_PROD_07: findProductDetailById thành công
    @Test
    @DisplayName("findProductDetailById - thành công trả về chi tiết sản phẩm")
    void findProductDetailById_success() {
        when(productRepo.findById(1L)).thenReturn(Optional.of(sampleProduct));
        when(productMapper.toDetailedResponse(sampleProduct)).thenReturn(sampleResponse);

        ProductResponse result = productService.findProductDetailById(1L);

        assertNotNull(result);
        assertEquals("1", result.getId());
    }

    // TC_PROD_08: filterAndPaginateProducts với các nhánh SortOrder
    @Test
    @DisplayName("filterAndPaginateProducts - phân trang và sắp xếp thành công")
    void filterAndPaginateProducts_success() {
        Page<Product> page = new PageImpl<>(List.of(sampleProduct));
        when(productRepo.filterProducts(any(), any(), anyBoolean(), anyList(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);
        when(productMapper.toBasicResponseList(anyList())).thenReturn(List.of(sampleResponse));

        PageResponse<ProductResponse> resultDesc = productService.filterAndPaginateProducts(
                "Hoa", BigDecimal.ZERO, BigDecimal.valueOf(500_000), List.of(1L), false, true, false, null, null, SortOrder.DESC, 0, 10);
        assertNotNull(resultDesc);
        assertEquals(1, resultDesc.getContent().size());

        // Kiểm tra nhánh SortOrder.PRICE_ASC
        PageResponse<ProductResponse> resultAsc = productService.filterAndPaginateProducts(
                null, null, null, null, null, null, null, null, null, SortOrder.PRICE_ASC, 0, 10);
        assertNotNull(resultAsc);
    }

    // TC_PROD_09: decreaseQuantity gọi repo
    @Test
    @DisplayName("decreaseQuantity - trừ số lượng kho thành công")
    void decreaseQuantity_callsRepo() {
        when(productRepo.decreaseQuantity(1L, 2)).thenReturn(1);

        Integer updated = productService.decreaseQuantity(1L, 2);

        assertEquals(1, updated);
        verify(productRepo).decreaseQuantity(1L, 2);
    }

    // TC_PROD_10: warmupCache chạy không lỗi
    @Test
    @DisplayName("warmupCache - chạy nạp cache không văng lỗi")
    void warmupCache_runsSuccessfully() {
        Page<Product> page = new PageImpl<>(List.of(sampleProduct));
        when(productRepo.filterProducts(any(), any(), anyBoolean(), anyList(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        productService.warmupCache(1);
        productService.warmupCache(0); // Nhánh times < 1

        verify(productRepo, times(1)).filterProducts(any(), any(), anyBoolean(), anyList(), any(), any(), any(), any(), any(), any(), any(Pageable.class));
    }
}
