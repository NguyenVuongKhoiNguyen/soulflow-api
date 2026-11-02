package com.poly.models.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

import com.poly.models.entities.ProductImage;
import com.poly.models.enums.SortOrder;
import com.poly.models.mappers.ProductImageMapper;
import com.poly.models.repositories.ProductImageRepository;
import com.poly.models.requests.ProductImageRequest;
import com.poly.models.responses.PageResponse;
import com.poly.models.responses.ProductImageResponse;
import com.poly.models.services.impl.ProductImageServiceImpl;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class ProductImageServiceTest {

    @Mock
    private ProductImageRepository productImageRepo;

    @Mock
    private ProductImageMapper productImageMapper;

    @InjectMocks
    private ProductImageServiceImpl productImageService;

    private ProductImage sampleImage;
    private ProductImageRequest sampleRequest;
    private ProductImageResponse sampleResponse;

    @BeforeEach
    void setUp() {
        sampleImage = new ProductImage();
        sampleImage.setId(1L);
        sampleImage.setName("flower-rose.jpg");
        sampleImage.setDeleted(false);

        sampleRequest = new ProductImageRequest();
        sampleRequest.setName("flower-rose.jpg");
        sampleRequest.setProductId(1L);

        sampleResponse = new ProductImageResponse();
        sampleResponse.setId("1");
        sampleResponse.setName("flower-rose.jpg");
        sampleResponse.setProductId("1");
    }

    // TC_IMG_01: Lưu thông tin ảnh sản phẩm thành công
    @Test
    @DisplayName("save - lưu ảnh sản phẩm thành công trả về ProductImageResponse")
    void save_validRequest_returnsProductImageResponse() {
        when(productImageMapper.toEntity(sampleRequest)).thenReturn(sampleImage);
        when(productImageRepo.save(sampleImage)).thenReturn(sampleImage);
        when(productImageMapper.toResponse(sampleImage)).thenReturn(sampleResponse);

        ProductImageResponse response = productImageService.save(sampleRequest);

        assertNotNull(response);
        assertEquals("1", response.getId());
        assertEquals("flower-rose.jpg", response.getName());
        verify(productImageRepo).save(sampleImage);
    }

    // Xem chi tiết ảnh theo ID
    @Test
    @DisplayName("findById - ID tồn tại trả về ProductImageResponse")
    void findById_existingId_returnsProductImageResponse() {
        when(productImageRepo.findById(1L)).thenReturn(Optional.of(sampleImage));
        when(productImageMapper.toResponse(sampleImage)).thenReturn(sampleResponse);

        ProductImageResponse response = productImageService.findById(1L);

        assertNotNull(response);
        assertEquals("1", response.getId());
    }

    @Test
    @DisplayName("findById - ID null ném IllegalArgumentException")
    void findById_nullId_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> productImageService.findById(null));
    }

    // TC_IMG_06: ID không tồn tại ném EntityNotFoundException
    @Test
    @DisplayName("findById - ID không tồn tại ném EntityNotFoundException")
    void findById_nonExistingId_throwsEntityNotFoundException() {
        when(productImageRepo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> productImageService.findById(999L));
    }

    // TC_IMG_05: Xóa mềm ảnh sản phẩm
    @Test
    @DisplayName("softDeleteById - gọi repository softDelete")
    void softDeleteById_callsRepositorySoftDelete() {
        doNothing().when(productImageRepo).softDelete(1L);

        assertDoesNotThrow(() -> productImageService.softDeleteById(1L));
        verify(productImageRepo).softDelete(1L);
    }

    // TC_IMG_07: Phân trang danh sách ảnh
    @Test
    @DisplayName("filterAndPagonateProductImages - sắp xếp ASC")
    void filterAndPagonateProductImages_sortAsc_returnsPageResponse() {
        Page<ProductImage> page = new PageImpl<>(List.of(sampleImage));
        when(productImageRepo.filterProductImages(eq("rose"), any(Pageable.class))).thenReturn(page);
        when(productImageMapper.toResponseList(any())).thenReturn(List.of(sampleResponse));

        PageResponse<ProductImageResponse> response = productImageService.filterAndPagonateProductImages("rose", SortOrder.ASC, 0, 10);

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
    }

    @Test
    @DisplayName("filterAndPagonateProductImages - sắp xếp DESC")
    void filterAndPagonateProductImages_sortDesc_returnsPageResponse() {
        Page<ProductImage> page = new PageImpl<>(List.of(sampleImage));
        when(productImageRepo.filterProductImages(isNull(), any(Pageable.class))).thenReturn(page);
        when(productImageMapper.toResponseList(any())).thenReturn(List.of(sampleResponse));

        PageResponse<ProductImageResponse> response = productImageService.filterAndPagonateProductImages(null, SortOrder.DESC, 0, 10);

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
    }
}
