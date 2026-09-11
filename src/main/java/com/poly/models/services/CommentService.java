package com.poly.models.services;

import java.time.LocalDateTime;

import com.poly.models.enums.SortOrder;
import com.poly.models.requests.CommentRequest;
import com.poly.models.responses.CommentResponse;
import com.poly.models.responses.PageResponse;

public interface CommentService {
	CommentResponse save(CommentRequest request);
	void softDeleteById(Long commentId);
	CommentResponse findById(Long commentId);
	PageResponse<CommentResponse> filterAndPaginateComments(
		String keyword, 
		LocalDateTime fromDate,
		LocalDateTime toDate, 
		Long productId,
		Long acocuntId,
		Boolean deleted, 
		SortOrder sortOrder, 
		Integer pageNumber, 
		Integer pageSize
	);
	void warmupCache(Integer times);
}
