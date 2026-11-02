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

import com.poly.models.entities.Comment;
import com.poly.models.enums.SortOrder;
import com.poly.models.mappers.CommentMapper;
import com.poly.models.repositories.CommentRepository;
import com.poly.models.requests.CommentRequest;
import com.poly.models.responses.CommentResponse;
import com.poly.models.responses.PageResponse;
import com.poly.models.services.impl.CommentServiceImpl;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepo;

    @Mock
    private CommentMapper commentMapper;

    @InjectMocks
    private CommentServiceImpl commentService;

    private Comment sampleComment;
    private CommentRequest sampleRequest;
    private CommentResponse sampleResponse;

    @BeforeEach
    void setUp() {
        sampleComment = new Comment();
        sampleComment.setId(1L);
        sampleComment.setContent("Hoa rất tươi và đẹp!");
        sampleComment.setDeleted(false);

        sampleRequest = new CommentRequest();
        sampleRequest.setContent("Hoa rất tươi và đẹp!");
        sampleRequest.setProductId(1L);
        sampleRequest.setAccountId(1L);

        sampleResponse = new CommentResponse();
        sampleResponse.setId("1");
        sampleResponse.setContent("Hoa rất tươi và đẹp!");
        sampleResponse.setProductId("1");
        sampleResponse.setReplyResponses(Collections.emptyList());
    }

    // TC_FB_01: Đăng bình luận thành công
    @Test
    @DisplayName("save - đăng bình luận thành công trả về CommentResponse")
    void save_validRequest_returnsCommentResponse() {
        when(commentMapper.toEntity(sampleRequest)).thenReturn(sampleComment);
        when(commentRepo.save(sampleComment)).thenReturn(sampleComment);
        when(commentMapper.toBasicResponse(sampleComment)).thenReturn(sampleResponse);

        CommentResponse response = commentService.save(sampleRequest);

        assertNotNull(response);
        assertEquals("1", response.getId());
        assertEquals("Hoa rất tươi và đẹp!", response.getContent());
        verify(commentRepo).save(sampleComment);
    }

    // TC_FB_06: Xóa mềm bình luận
    @Test
    @DisplayName("softDeleteById - gọi repository softDelete")
    void softDeleteById_callsRepositorySoftDelete() {
        doNothing().when(commentRepo).softDelete(1L);

        assertDoesNotThrow(() -> commentService.softDeleteById(1L));
        verify(commentRepo).softDelete(1L);
    }

    // TC_FB_07: Xem chi tiết bình luận theo ID
    @Test
    @DisplayName("findById - ID tồn tại trả về CommentResponse chi tiết")
    void findById_existingId_returnsCommentResponse() {
        when(commentRepo.findById(1L)).thenReturn(Optional.of(sampleComment));
        when(commentMapper.toDetailedResponse(sampleComment)).thenReturn(sampleResponse);

        CommentResponse response = commentService.findById(1L);

        assertNotNull(response);
        assertEquals("1", response.getId());
    }

    @Test
    @DisplayName("findById - ID null ném IllegalArgumentException")
    void findById_nullId_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> commentService.findById(null));
    }

    @Test
    @DisplayName("findById - ID không tồn tại ném EntityNotFoundException")
    void findById_nonExistingId_throwsEntityNotFoundException() {
        when(commentRepo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> commentService.findById(999L));
    }

    // TC_FB_08: Lọc và phân trang bình luận công khai
    @Test
    @DisplayName("filterAndPaginateComments - sắp xếp ASC")
    void filterAndPaginateComments_sortAsc_returnsPageResponse() {
        Page<Comment> page = new PageImpl<>(List.of(sampleComment));
        when(commentRepo.filterComments(any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);
        when(commentMapper.toBasicResponseList(any())).thenReturn(List.of(sampleResponse));

        PageResponse<CommentResponse> response = commentService.filterAndPaginateComments(
                null, "tươi", null, null, 1L, null, false, SortOrder.ASC, 0, 10
        );

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
    }

    @Test
    @DisplayName("filterAndPaginateComments - sắp xếp DESC")
    void filterAndPaginateComments_sortDesc_returnsPageResponse() {
        Page<Comment> page = new PageImpl<>(List.of(sampleComment));
        when(commentRepo.filterComments(any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);
        when(commentMapper.toBasicResponseList(any())).thenReturn(List.of(sampleResponse));

        PageResponse<CommentResponse> response = commentService.filterAndPaginateComments(
                null, null, null, null, null, null, false, SortOrder.DESC, 0, 10
        );

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
    }
}
