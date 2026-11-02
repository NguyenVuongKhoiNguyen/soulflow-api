package com.poly.models.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.poly.models.entities.Category;
import com.poly.models.enums.SortOrder;
import com.poly.models.mappers.CategoryMappper;
import com.poly.models.repositories.CategoryRepository;
import com.poly.models.requests.CategoryRequest;
import com.poly.models.responses.CategoryResponse;
import com.poly.models.responses.PageResponse;
import com.poly.models.services.impl.CategoryServiceImpl;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepo;

    @Mock
    private CategoryMappper categoryMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category sampleCategory;
    private CategoryRequest sampleRequest;
    private CategoryResponse sampleResponse;

    @BeforeEach
    void setUp() {
        sampleCategory = new Category();
        sampleCategory.setId(1L);
        sampleCategory.setName("Hoa Sinh Nhật");
        sampleCategory.setDescription("Các mẫu hoa tươi chúc mừng sinh nhật");
        sampleCategory.setDeleted(false);

        sampleRequest = new CategoryRequest();
        sampleRequest.setName("Hoa Sinh Nhật");
        sampleRequest.setDescription("Các mẫu hoa tươi chúc mừng sinh nhật");

        sampleResponse = new CategoryResponse();
        sampleResponse.setId("1");
        sampleResponse.setName("Hoa Sinh Nhật");
        sampleResponse.setDescription("Các mẫu hoa tươi chúc mừng sinh nhật");
    }

    // TC_CAT_05 / TC_CAT_06: Admin tạo danh mục mới
    @Test
    @DisplayName("save - tạo mới hoặc cập nhật danh mục thành công")
    void save_validRequest_returnsSavedCategoryResponse() {
        when(categoryMapper.toEntity(any(CategoryRequest.class))).thenReturn(sampleCategory);
        when(categoryRepo.save(any(Category.class))).thenReturn(sampleCategory);
        when(categoryMapper.toBasicResponse(any(Category.class))).thenReturn(sampleResponse);

        CategoryResponse result = categoryService.save(sampleRequest);

        assertNotNull(result);
        assertEquals("1", result.getId());
        assertEquals("Hoa Sinh Nhật", result.getName());
        verify(categoryRepo, times(1)).save(sampleCategory);
    }

    // TC_CAT_11: Xóa mềm danh mục theo ID hợp lệ
    @Test
    @DisplayName("softDeleteById - gọi repository thực hiện softDelete")
    void softDeleteById_validId_callsRepositorySoftDelete() {
        doNothing().when(categoryRepo).softDelete(1L);

        assertDoesNotThrow(() -> categoryService.softDeleteById(1L));
        verify(categoryRepo, times(1)).softDelete(1L);
    }

    // TC_CAT_14: Lấy chi tiết danh mục theo ID tồn tại
    @Test
    @DisplayName("findById - ID tồn tại trả về CategoryResponse")
    void findById_existingId_returnsCategoryResponse() {
        when(categoryRepo.findById(1L)).thenReturn(Optional.of(sampleCategory));
        when(categoryMapper.toBasicResponse(sampleCategory)).thenReturn(sampleResponse);

        CategoryResponse result = categoryService.findById(1L);

        assertNotNull(result);
        assertEquals("1", result.getId());
        assertEquals("Hoa Sinh Nhật", result.getName());
    }

    // TC_CAT_15: Lấy chi tiết danh mục với ID không tồn tại
    @Test
    @DisplayName("findById - ID không tồn tại ném EntityNotFoundException")
    void findById_nonExistingId_throwsEntityNotFoundException() {
        when(categoryRepo.findById(999L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> {
            categoryService.findById(999L);
        });
        assertTrue(ex.getMessage().contains("Category not found with id: 999"));
    }

    @Test
    @DisplayName("findById - ID null ném IllegalArgumentException")
    void findById_nullId_throwsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            categoryService.findById(null);
        });
        assertTrue(ex.getMessage().contains("Can't not find category when id is null"));
    }

    // TC_CAT_01 / TC_CAT_04: Lấy danh sách danh mục công khai
    @Test
    @DisplayName("findAll - trả về danh sách danh mục chưa bị xóa")
    void findAll_returnsListOfNonDeletedCategories() {
        when(categoryRepo.findAllByDeletedFalse()).thenReturn(List.of(sampleCategory));
        when(categoryMapper.toBasicResponseList(any())).thenReturn(List.of(sampleResponse));

        List<CategoryResponse> results = categoryService.findAll();

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Hoa Sinh Nhật", results.get(0).getName());
    }

    @Test
    @DisplayName("findAll - khi DB rỗng trả về danh sách trống")
    void findAll_emptyDb_returnsEmptyList() {
        when(categoryRepo.findAllByDeletedFalse()).thenReturn(Collections.emptyList());
        when(categoryMapper.toBasicResponseList(any())).thenReturn(Collections.emptyList());

        List<CategoryResponse> results = categoryService.findAll();

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    // TC_CAT_16 / TC_CAT_17 / TC_CAT_18: Lọc và phân trang danh mục
    @Test
    @DisplayName("filterAndPaginateCategories - sắp xếp ASC")
    void filterAndPaginateCategories_sortOrderAsc_returnsPageResponse() {
        Page<Category> page = new PageImpl<>(List.of(sampleCategory));
        when(categoryRepo.filterCategories(eq("Hoa"), eq(false), any(Pageable.class))).thenReturn(page);
        when(categoryMapper.toBasicResponseList(any())).thenReturn(List.of(sampleResponse));

        PageResponse<CategoryResponse> result = categoryService.filterAndPaginateCategories(
                "Hoa", false, SortOrder.ASC, 0, 10
        );

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(categoryRepo).filterCategories(eq("Hoa"), eq(false), any(Pageable.class));
    }

    @Test
    @DisplayName("filterAndPaginateCategories - sắp xếp DESC")
    void filterAndPaginateCategories_sortOrderDesc_returnsPageResponse() {
        Page<Category> page = new PageImpl<>(List.of(sampleCategory));
        when(categoryRepo.filterCategories(isNull(), isNull(), any(Pageable.class))).thenReturn(page);
        when(categoryMapper.toBasicResponseList(any())).thenReturn(List.of(sampleResponse));

        PageResponse<CategoryResponse> result = categoryService.filterAndPaginateCategories(
                null, null, SortOrder.DESC, 0, 10
        );

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }
}
