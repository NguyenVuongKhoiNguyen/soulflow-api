package com.poly.models.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.poly.models.entities.Discount;
import com.poly.models.enums.SortOrder;
import com.poly.models.mappers.DiscountMapper;
import com.poly.models.mappers.ProductMapper;
import com.poly.models.repositories.DiscountRepository;
import com.poly.models.repositories.ProductRepository;
import com.poly.models.requests.DiscountRequest;
import com.poly.models.responses.DiscountResponse;
import com.poly.models.responses.PageResponse;
import com.poly.models.services.impl.DiscountServiceImpl;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class DiscountServiceTest {

    @Mock
    private DiscountRepository discountRepo;

    @Mock
    private DiscountMapper discountMapper;

    @Mock
    private ProductRepository productRepo;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private CacheManager cacheManager;

    @InjectMocks
    private DiscountServiceImpl discountService;

    private Discount sampleDiscount;
    private DiscountRequest sampleRequest;
    private DiscountResponse sampleResponse;

    @BeforeEach
    void setUp() {
        sampleDiscount = new Discount();
        sampleDiscount.setId(1L);
        sampleDiscount.setPercentage(BigDecimal.valueOf(15));
        sampleDiscount.setDescription("Giảm giá 15% mùa hè");
        sampleDiscount.setExpiredDate(LocalDateTime.now().plusDays(10));
        sampleDiscount.setExpired(false);
        sampleDiscount.setProducts(Collections.emptyList());

        sampleRequest = new DiscountRequest();
        sampleRequest.setPercentage(BigDecimal.valueOf(15));
        sampleRequest.setDescription("Giảm giá 15% mùa hè");
        sampleRequest.setExpiredDate(LocalDateTime.now().plusDays(10));

        sampleResponse = new DiscountResponse();
        sampleResponse.setId("1");
        sampleResponse.setPercentage("15");
        sampleResponse.setDescription("Giảm giá 15% mùa hè");
    }

    // TC_DISC_01 / TC_DISC_05: Tạo mã giảm giá hợp lệ
    @Test
    @DisplayName("save - tạo mã giảm giá còn hạn (expired = false)")
    void save_futureExpiredDate_returnsDiscountResponse() {
        when(discountMapper.toEntity(sampleRequest)).thenReturn(sampleDiscount);
        when(discountRepo.save(sampleDiscount)).thenReturn(sampleDiscount);
        when(discountMapper.toResponse(sampleDiscount)).thenReturn(sampleResponse);

        DiscountResponse response = discountService.save(sampleRequest);

        assertNotNull(response);
        assertEquals("1", response.getId());
        assertFalse(sampleDiscount.getExpired());
        verify(discountRepo).save(sampleDiscount);
    }

    @Test
    @DisplayName("save - tạo mã giảm giá với ngày hết hạn trong quá khứ (expired = true)")
    void save_pastExpiredDate_setsExpiredTrue() {
        sampleDiscount.setExpiredDate(LocalDateTime.now().minusDays(1));
        sampleRequest.setExpiredDate(LocalDateTime.now().minusDays(1));

        when(discountMapper.toEntity(sampleRequest)).thenReturn(sampleDiscount);
        when(discountRepo.save(sampleDiscount)).thenReturn(sampleDiscount);
        when(discountMapper.toResponse(sampleDiscount)).thenReturn(sampleResponse);

        DiscountResponse response = discountService.save(sampleRequest);

        assertNotNull(response);
        assertTrue(sampleDiscount.getExpired());
    }

    // TC_DISC_11: Xóa mềm mã giảm giá
    @Test
    @DisplayName("softDeleteById - gọi repository softDelete")
    void softDeleteById_callsRepositorySoftDelete() {
        when(discountRepo.softDelete(1L)).thenReturn(1);

        assertDoesNotThrow(() -> discountService.softDeleteById(1L));
        verify(discountRepo).softDelete(1L);
    }

    // TC_DISC_14: Xem chi tiết discount theo ID tồn tại
    @Test
    @DisplayName("findById - ID tồn tại trả về DiscountResponse")
    void findById_existingId_returnsDiscountResponse() {
        when(discountRepo.findById(1L)).thenReturn(Optional.of(sampleDiscount));
        when(discountMapper.toResponse(sampleDiscount)).thenReturn(sampleResponse);
        when(productMapper.toBasicResponseList(any())).thenReturn(Collections.emptyList());

        DiscountResponse response = discountService.findById(1L);

        assertNotNull(response);
        assertEquals("1", response.getId());
    }

    // TC_DISC_15: Xem chi tiết discount ID không tồn tại ném EntityNotFoundException
    @Test
    @DisplayName("findById - ID không tồn tại ném EntityNotFoundException")
    void findById_nonExistingId_throwsEntityNotFoundException() {
        when(discountRepo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> discountService.findById(999L));
    }

    @Test
    @DisplayName("findById - ID null ném IllegalArgumentException")
    void findById_nullId_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> discountService.findById(null));
    }

    // TC_DISC_16: Lọc và phân trang discount
    @Test
    @DisplayName("filterAndPaginateDiscounts - trả về PageResponse")
    void filterAndPaginateDiscounts_returnsPageResponse() {
        Page<Discount> page = new PageImpl<>(List.of(sampleDiscount));
        when(discountRepo.filterDiscounts(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);
        when(discountMapper.toResponseList(any())).thenReturn(List.of(sampleResponse));

        PageResponse<DiscountResponse> response = discountService.filterAndPaginateDiscounts(
                "hè", null, null, false, false, SortOrder.DESC, 0, 10
        );

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
    }

    // Kiểm tra hết hạn trước phân trang
    @Test
    @DisplayName("checkAndExpireBeforePagination - xóa cache khi có bản ghi bị hết hạn")
    void checkAndExpireBeforePagination_effectedRows_clearsCaches() {
        when(discountRepo.checkAndExpireBeforePagination(any(), any(), any(), any(), any())).thenReturn(2);
        Cache mockCache = mock(Cache.class);
        when(cacheManager.getCache(anyString())).thenReturn(mockCache);

        discountService.checkAndExpireBeforePagination(null, null, null, false, false);

        verify(mockCache, atLeastOnce()).clear();
    }
}
