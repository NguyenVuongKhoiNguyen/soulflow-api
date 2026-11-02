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

import com.poly.models.entities.Store;
import com.poly.models.mappers.StoreMapper;
import com.poly.models.repositories.StoreRepository;
import com.poly.models.requests.StoreRequest;
import com.poly.models.responses.PageResponse;
import com.poly.models.responses.StoreResponse;
import com.poly.models.services.impl.StoreServiceImpl;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class StoreServiceTest {

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private StoreMapper storeMapper;

    @InjectMocks
    private StoreServiceImpl storeService;

    private Store sampleStore;
    private StoreRequest sampleRequest;
    private StoreResponse sampleResponse;

    @BeforeEach
    void setUp() {
        sampleStore = new Store();
        sampleStore.setId(1L);
        sampleStore.setName("Chi nhánh Q1");
        sampleStore.setPhone("0901234567");
        sampleStore.setAddress("123 Lê Lợi, Q1, TP.HCM");

        sampleRequest = new StoreRequest();
        sampleRequest.setName("Chi nhánh Q1");
        sampleRequest.setPhone("0901234567");
        sampleRequest.setAddress("123 Lê Lợi, Q1, TP.HCM");

        sampleResponse = new StoreResponse();
        sampleResponse.setId("1");
        sampleResponse.setName("Chi nhánh Q1");
        sampleResponse.setPhone("0901234567");
        sampleResponse.setAddress("123 Lê Lợi, Q1, TP.HCM");
    }

    // TC_STR_01 / TC_STR_05: Tạo mới chi nhánh thành công
    @Test
    @DisplayName("save - tạo mới store với id null thành công")
    void save_newStore_returnsStoreResponse() {
        Store newStore = new Store();
        newStore.setName("Chi nhánh Q1");
        newStore.setPhone("0901234567");
        newStore.setAddress("123 Lê Lợi, Q1, TP.HCM");

        when(storeMapper.toEntity(sampleRequest)).thenReturn(newStore);
        when(storeRepository.save(newStore)).thenReturn(sampleStore);
        when(storeMapper.toResponse(sampleStore)).thenReturn(sampleResponse);

        StoreResponse response = storeService.save(sampleRequest);

        assertNotNull(response);
        assertEquals("1", response.getId());
        assertEquals("Chi nhánh Q1", response.getName());
        verify(storeRepository).save(newStore);
    }

    // TC_STR_08: Cập nhật store với ID tồn tại thành công
    @Test
    @DisplayName("save - cập nhật store với ID tồn tại thành công")
    void save_existingStore_updatesSuccessfully() {
        sampleRequest.setId(1L);
        when(storeMapper.toEntity(sampleRequest)).thenReturn(sampleStore);
        when(storeRepository.existsById(1L)).thenReturn(true);
        when(storeRepository.save(sampleStore)).thenReturn(sampleStore);
        when(storeMapper.toResponse(sampleStore)).thenReturn(sampleResponse);

        StoreResponse response = storeService.save(sampleRequest);

        assertNotNull(response);
        assertEquals("1", response.getId());
        verify(storeRepository).existsById(1L);
    }

    // TC_STR_09: Cập nhật store với ID không tồn tại ném EntityNotFoundException
    @Test
    @DisplayName("save - cập nhật store với ID không tồn tại ném EntityNotFoundException")
    void save_nonExistingStoreId_throwsEntityNotFoundException() {
        Store nonExisting = new Store();
        nonExisting.setId(999L);
        when(storeMapper.toEntity(sampleRequest)).thenReturn(nonExisting);
        when(storeRepository.existsById(999L)).thenReturn(false);

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> {
            storeService.save(sampleRequest);
        });
        assertTrue(ex.getMessage().contains("Store not found with id: 999"));
    }

    // TC_STR_14: Xem chi tiết store theo ID hợp lệ
    @Test
    @DisplayName("findById - ID hợp lệ trả về StoreResponse")
    void findById_validId_returnsStoreResponse() {
        when(storeRepository.findById(1L)).thenReturn(Optional.of(sampleStore));
        when(storeMapper.toResponse(sampleStore)).thenReturn(sampleResponse);

        StoreResponse response = storeService.findById(1L);

        assertNotNull(response);
        assertEquals("1", response.getId());
    }

    // TC_STR_15: Xem chi tiết store ID không tồn tại ném EntityNotFoundException
    @Test
    @DisplayName("findById - ID không tồn tại ném EntityNotFoundException")
    void findById_nonExistingId_throwsEntityNotFoundException() {
        when(storeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> storeService.findById(999L));
    }

    @Test
    @DisplayName("findById - ID null ném IllegalArgumentException")
    void findById_nullId_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> storeService.findById(null));
    }

    // TC_STR_04: Lấy toàn bộ danh sách chi nhánh
    @Test
    @DisplayName("findAll - trả về danh sách chi nhánh")
    void findAll_returnsListOfStores() {
        when(storeRepository.findAll()).thenReturn(List.of(sampleStore));
        when(storeMapper.toResponseList(any())).thenReturn(List.of(sampleResponse));

        List<StoreResponse> results = storeService.findAll();

        assertNotNull(results);
        assertEquals(1, results.size());
    }

    // TC_STR_16: Lọc và phân trang chi nhánh
    @Test
    @DisplayName("filterAndPaginateStores - trả về PageResponse")
    void filterAndPaginateStores_returnsPageResponse() {
        Page<Store> page = new PageImpl<>(List.of(sampleStore));
        when(storeRepository.filterStores(eq(1L), eq("Q1"), any(Pageable.class))).thenReturn(page);
        when(storeMapper.toResponseList(any())).thenReturn(List.of(sampleResponse));

        PageResponse<StoreResponse> result = storeService.filterAndPaginateStores(1L, "Q1", 0, 10);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    // TC_STR_11: Xóa chi nhánh tồn tại thành công
    @Test
    @DisplayName("deleteById - ID tồn tại gọi repository deleteById")
    void deleteById_existingId_deletesSuccessfully() {
        when(storeRepository.existsById(1L)).thenReturn(true);
        doNothing().when(storeRepository).deleteById(1L);

        assertDoesNotThrow(() -> storeService.deleteById(1L));
        verify(storeRepository).deleteById(1L);
    }

    // TC_STR_12: Xóa chi nhánh không tồn tại ném EntityNotFoundException
    @Test
    @DisplayName("deleteById - ID không tồn tại ném EntityNotFoundException")
    void deleteById_nonExistingId_throwsEntityNotFoundException() {
        when(storeRepository.existsById(999L)).thenReturn(false);

        assertThrows(EntityNotFoundException.class, () -> storeService.deleteById(999L));
    }
}
