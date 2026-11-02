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
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.poly.models.entities.Order;
import com.poly.models.entities.Store;
import com.poly.models.enums.OrderStatus;
import com.poly.models.enums.SortOrder;
import com.poly.models.mappers.OrderMapper;
import com.poly.models.repositories.OrderDetailRepository;
import com.poly.models.repositories.OrderRepository;
import com.poly.models.repositories.StoreRepository;
import com.poly.models.requests.AdminOrderUpdateRequest;
import com.poly.models.requests.OrderRequest;
import com.poly.models.responses.OrderResponse;
import com.poly.models.responses.PageResponse;
import com.poly.models.services.impl.OrderServiceImpl;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderRepository orderRepo;

    @Mock
    private OrderDetailRepository orderDetailRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private ShippingFeeService shippingFeeService;

    @Mock
    private CacheManager cacheManager;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order sampleOrder;
    private OrderRequest sampleRequest;
    private OrderResponse sampleResponse;
    private Store sampleStore;

    @BeforeEach
    void setUp() {
        sampleStore = new Store();
        sampleStore.setId(1L);
        sampleStore.setAddress("123 Flower St");

        sampleOrder = new Order();
        sampleOrder.setId(1L);
        sampleOrder.setFullname("Nguyen Van A");
        sampleOrder.setPhone("0901234567");
        sampleOrder.setAddress("456 Customer Ave");
        sampleOrder.setStatus(OrderStatus.PENDING);
        sampleOrder.setStore(sampleStore);
        sampleOrder.setDeleted(false);

        sampleRequest = new OrderRequest();
        sampleRequest.setFullname("Nguyen Van A");
        sampleRequest.setPhone("0901234567");
        sampleRequest.setAddress("456 Customer Ave");

        sampleResponse = new OrderResponse();
        sampleResponse.setId("1");
        sampleResponse.setFullname("Nguyen Van A");
        sampleResponse.setStatus("PENDING");
    }

    // TC_ORD_01: save đơn hàng mới có tính phí ship tự động
    @Test
    @DisplayName("save - đơn hàng mới tính phí ship và lưu thành công")
    void save_newOrderWithStore_calculatesShippingFee() {
        sampleRequest.setId(null);
        when(orderMapper.toEntity(sampleRequest)).thenReturn(sampleOrder);
        when(shippingFeeService.calculateFee(anyString(), anyString())).thenReturn(BigDecimal.valueOf(30_000));
        when(orderRepo.save(sampleOrder)).thenReturn(sampleOrder);
        when(orderMapper.toResponse(sampleOrder)).thenReturn(sampleResponse);

        OrderResponse result = orderService.save(sampleRequest);

        assertNotNull(result);
        assertEquals("1", result.getId());
        verify(shippingFeeService).calculateFee(eq("123 Flower St"), eq("456 Customer Ave"));
        verify(orderRepo).save(sampleOrder);
    }

    // TC_ORD_02: softDeleteById gọi repo
    @Test
    @DisplayName("softDeleteById - xóa mềm đơn hàng gọi repo")
    void softDeleteById_validId_callsRepository() {
        when(orderRepo.softDelete(1L)).thenReturn(1);

        orderService.softDeleteById(1L);

        verify(orderRepo).softDelete(1L);
    }

    // TC_ORD_03: findById ném lỗi khi id null hoặc không tìm thấy
    @Test
    @DisplayName("findById - id null ném IllegalArgumentException, không tìm thấy ném EntityNotFoundException")
    void findById_exceptions() {
        assertThrows(IllegalArgumentException.class, () -> orderService.findById(null));

        when(orderRepo.findById(999L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> orderService.findById(999L));
    }

    // TC_ORD_04: findById thành công
    @Test
    @DisplayName("findById - tìm thấy trả về OrderResponse")
    void findById_success() {
        when(orderRepo.findById(1L)).thenReturn(Optional.of(sampleOrder));
        when(orderMapper.toResponse(sampleOrder)).thenReturn(sampleResponse);

        OrderResponse result = orderService.findById(1L);

        assertNotNull(result);
        assertEquals("1", result.getId());
    }

    // TC_ORD_05: updateAdminInfo khi đơn đã CANCELLED cố mở lại ném 409 CONFLICT
    @Test
    @DisplayName("updateAdminInfo - đơn đã hủy không được mở lại ném 409 CONFLICT")
    void updateAdminInfo_cancelledOrderReopen_throwsConflict409() {
        sampleOrder.setStatus(OrderStatus.CANCELLED);
        when(orderRepo.findById(1L)).thenReturn(Optional.of(sampleOrder));

        AdminOrderUpdateRequest req = new AdminOrderUpdateRequest();
        req.setStatus(OrderStatus.DELIVERED);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> orderService.updateAdminInfo(1L, req));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("A cancelled order cannot be reopened"));
    }

    // TC_ORD_06: updateAdminInfo hủy đơn thì phục hồi số lượng tồn kho sản phẩm
    @Test
    @DisplayName("updateAdminInfo - chuyển sang CANCELLED gọi phục hồi tồn kho sản phẩm")
    void updateAdminInfo_cancelOrder_restoresProductQuantity() {
        sampleOrder.setStatus(OrderStatus.PENDING);
        when(orderRepo.findById(1L)).thenReturn(Optional.of(sampleOrder));
        when(orderRepo.save(sampleOrder)).thenReturn(sampleOrder);
        when(orderMapper.toResponse(sampleOrder)).thenReturn(sampleResponse);

        AdminOrderUpdateRequest req = new AdminOrderUpdateRequest();
        req.setStatus(OrderStatus.CANCELLED);
        req.setFullname("Nguyen Van A");
        req.setPhone("0901234567");
        req.setAddress("456 Customer Ave");

        OrderResponse res = orderService.updateAdminInfo(1L, req);

        assertNotNull(res);
        verify(orderDetailRepository).restoreProductQuantity(1L);
    }

    // TC_ORD_07: updateAdminInfo không tìm thấy storeId ném EntityNotFoundException
    @Test
    @DisplayName("updateAdminInfo - storeId không tồn tại ném EntityNotFoundException")
    void updateAdminInfo_invalidStore_throwsException() {
        sampleOrder.setStatus(OrderStatus.PENDING);
        when(orderRepo.findById(1L)).thenReturn(Optional.of(sampleOrder));
        when(storeRepository.findById(99L)).thenReturn(Optional.empty());

        AdminOrderUpdateRequest req = new AdminOrderUpdateRequest();
        req.setStatus(OrderStatus.PENDING);
        req.setStoreId(99L);

        assertThrows(EntityNotFoundException.class, () -> orderService.updateAdminInfo(1L, req));
    }

    // TC_ORD_08: findMine trả về danh sách đơn của user
    @Test
    @DisplayName("findMine - trả về danh sách đơn hàng của người dùng hiện tại")
    void findMine_success() {
        Page<Order> page = new PageImpl<>(List.of(sampleOrder));
        when(orderRepo.findActiveByUsername(eq("user1"), any(Pageable.class))).thenReturn(page);
        when(orderMapper.toResponseList(anyList())).thenReturn(List.of(sampleResponse));

        PageResponse<OrderResponse> result = orderService.findMine("user1", 0, 10);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    // TC_ORD_09: findMineById không tìm thấy ném EntityNotFoundException
    @Test
    @DisplayName("findMineById - không tìm thấy ném EntityNotFoundException")
    void findMineById_notFound_throwsException() {
        when(orderRepo.findActiveByIdAndUsername(999L, "user1")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> orderService.findMineById("user1", 999L));
    }

    // TC_ORD_10: filterAndPaginateOrders phân trang thành công
    @Test
    @DisplayName("filterAndPaginateOrders - lọc và phân trang đơn hàng")
    void filterAndPaginateOrders_success() {
        Page<Order> page = new PageImpl<>(List.of(sampleOrder));
        when(orderRepo.filterOrders(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);
        when(orderMapper.toResponseList(anyList())).thenReturn(List.of(sampleResponse));

        PageResponse<OrderResponse> result = orderService.filterAndPaginateOrders(
                "Nguyen", null, null, OrderStatus.PENDING, false, false, SortOrder.DESC, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    // TC_ORD_11: checkAndExpireBeforePagination xóa cache khi có thay đổi
    @Test
    @DisplayName("checkAndExpireBeforePagination - xóa cache khi có đơn hết hạn")
    void checkAndExpireBeforePagination_clearsCache() {
        when(orderRepo.checkAndExpireBeforePagination(any(), any(), any(), any(), any()))
                .thenReturn(3);
        Cache mockCache = mock(Cache.class);
        when(cacheManager.getCache(anyString())).thenReturn(mockCache);

        orderService.checkAndExpireBeforePagination("Nguyen", null, null, OrderStatus.PENDING, false, false);

        verify(mockCache, atLeastOnce()).clear();
    }
}
