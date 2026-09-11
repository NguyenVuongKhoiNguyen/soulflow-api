package com.poly.models.services;

import java.time.LocalDateTime;
import java.util.List;

import com.poly.models.enums.SortOrder;
import com.poly.models.requests.ReplyRequest;
import com.poly.models.responses.PageResponse;
import com.poly.models.responses.ReplyResponse;

public interface ReplyService {
	ReplyResponse save(ReplyRequest request);
	void softDeleteById(Long replyId);
	ReplyResponse findById(Long replyId);
	List<ReplyResponse> findAll();
	PageResponse<ReplyResponse> filterAndPaginateReply(
			String keyword,
            LocalDateTime fromDate,
            LocalDateTime toDate,
			Long accountId,
			Long commentId,
            Boolean deleted,
			SortOrder sortOrder,
            Integer pageNumber,
			Integer pageSize
	);
	void warmupCache(Integer times);
}
