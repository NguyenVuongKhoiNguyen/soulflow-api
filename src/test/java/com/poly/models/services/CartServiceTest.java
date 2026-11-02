package com.poly.models.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
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

import com.poly.models.entities.Cart;
import com.poly.models.enums.SortOrder;
import com.poly.models.mappers.CartMapper;
import com.poly.models.repositories.CartRepository;
import com.poly.models.requests.CartRequest;
import com.poly.models.responses.CartResponse;
import com.poly.models.responses.PageResponse;
import com.poly.models.services.impl.CartServiceImpl;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepo;

    @Mock
    private CartMapper cartMapper;

    @Mock
    private CacheManager cacheManager;

    @InjectMocks
    private CartServiceImpl cartService;

    private Cart sampleCart;
    private CartRequest sampleRequest;
    private CartResponse sampleResponse;

    @BeforeEach
    void setUp() {
        sampleCart = new Cart();
        sampleCart.setId(1L);
        sampleCart.setItems(new ArrayList<>());
        sampleCart.setDeleted(false);

        sampleRequest = new CartRequest();
        sampleRequest.setAccountId(1L);
        sampleRequest.setItemRequests(Collections.emptyList());

        sampleResponse = new CartResponse();
        sampleResponse.setId("1");
        sampleResponse.setAccountId("1");
        sampleResponse.setTotal("250000");
    }

    // TC_CART_01: Tạo giỏ hàng mới (id = null)
    @Test
    @DisplayName("save - tạo giỏ hàng mới khi id null thành công")
    void save_newCart_returnsCartResponse() {
        when(cartMapper.toEntity(sampleRequest)).thenReturn(sampleCart);
        when(cartRepo.save(sampleCart)).thenReturn(sampleCart);
        when(cartMapper.toResponse(sampleCart)).thenReturn(sampleResponse);

        CartResponse response = cartService.save(sampleRequest);

        assertNotNull(response);
        assertEquals("1", response.getId());
        verify(cartRepo).save(sampleCart);
    }

    // Cập nhật giỏ hàng khi ID tồn tại
    @Test
    @DisplayName("save - cập nhật giỏ hàng đã có thành công")
    void save_existingCart_updatesItemsSuccessfully() {
        sampleRequest.setId(1L);
        Cart existingCart = new Cart();
        existingCart.setId(1L);
        existingCart.setItems(new ArrayList<>());

        when(cartMapper.toEntity(sampleRequest)).thenReturn(sampleCart);
        when(cartRepo.findById(1L)).thenReturn(Optional.of(existingCart));
        when(cartMapper.toResponse(existingCart)).thenReturn(sampleResponse);

        CartResponse response = cartService.save(sampleRequest);

        assertNotNull(response);
        assertEquals("1", response.getId());
        verify(cartRepo).flush();
    }

    // Cập nhật giỏ hàng với ID không tồn tại ném EntityNotFoundException
    @Test
    @DisplayName("save - cập nhật giỏ hàng không tồn tại ném EntityNotFoundException")
    void save_nonExistingCartId_throwsEntityNotFoundException() {
        sampleRequest.setId(999L);
        when(cartMapper.toEntity(sampleRequest)).thenReturn(sampleCart);
        when(cartRepo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> cartService.save(sampleRequest));
    }

    // TC_CART_05: Xóa mềm giỏ hàng
    @Test
    @DisplayName("softDeleteById - gọi repository softDelete")
    void softDeleteById_callsRepositorySoftDelete() {
        when(cartRepo.softDelete(1L)).thenReturn(1);

        assertDoesNotThrow(() -> cartService.softDeleteById(1L));
        verify(cartRepo).softDelete(1L);
    }

    // TC_CART_09: Xem chi tiết giỏ hàng theo ID
    @Test
    @DisplayName("findById - ID tồn tại trả về CartResponse")
    void findById_existingId_returnsCartResponse() {
        when(cartRepo.findById(1L)).thenReturn(Optional.of(sampleCart));
        when(cartMapper.toResponse(sampleCart)).thenReturn(sampleResponse);

        CartResponse response = cartService.findById(1L);

        assertNotNull(response);
        assertEquals("1", response.getId());
    }

    @Test
    @DisplayName("findById - ID null ném IllegalArgumentException")
    void findById_nullId_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> cartService.findById(null));
    }

    @Test
    @DisplayName("findById - ID không tồn tại ném EntityNotFoundException")
    void findById_nonExistingId_throwsEntityNotFoundException() {
        when(cartRepo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> cartService.findById(999L));
    }

    // TC_CART_12: Phân trang giỏ hàng
    @Test
    @DisplayName("filterAndPaginateCarts - trả về PageResponse")
    void filterAndPaginateCarts_returnsPageResponse() {
        Page<Cart> page = new PageImpl<>(List.of(sampleCart));
        when(cartRepo.filterCarts(eq(1L), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);
        when(cartMapper.toResponseList(any())).thenReturn(List.of(sampleResponse));

        PageResponse<CartResponse> response = cartService.filterAndPaginateCarts(
                1L, null, null, null, false, false, SortOrder.DESC, 0, 10
        );

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
    }

    // Kiểm tra hết hạn giỏ hàng trước phân trang
    @Test
    @DisplayName("checkAndExpireBeforePagination - xóa cache khi có giỏ hàng hết hạn")
    void checkAndExpireBeforePagination_clearsCache() {
        when(cartRepo.checkAndExpireBeforePagination(any(), any(), any(), any(), any())).thenReturn(1);
        Cache mockCache = mock(Cache.class);
        when(cacheManager.getCache(anyString())).thenReturn(mockCache);

        cartService.checkAndExpireBeforePagination(null, null, null, false, false);

        verify(mockCache, atLeastOnce()).clear();
    }
}
