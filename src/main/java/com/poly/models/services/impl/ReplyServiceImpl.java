package com.poly.models.services.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import com.poly.models.entities.Reply;
import com.poly.models.enums.SortOrder;
import com.poly.models.mappers.ReplyMapper;
import com.poly.models.repositories.ReplyRepository;
import com.poly.models.requests.ReplyRequest;
import com.poly.models.responses.PageResponse;
import com.poly.models.responses.ReplyResponse;
import com.poly.models.services.ReplyService;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReplyServiceImpl implements ReplyService {
	
	private final ReplyRepository replyRepo;
	private final ReplyMapper replyMapper;

	@Override
	@Transactional
	@Caching(evict = {
		@CacheEvict(value = "replyPages", allEntries = true),
		@CacheEvict(value = "commentList", key = "#request.commentId"),
		@CacheEvict(value = "productDetailList", allEntries = true)
	})
	public ReplyResponse save(ReplyRequest request) {
		Reply reply = replyMapper.toEntity(request);
		Reply saved = replyRepo.save(reply);
		return replyMapper.toResponse(saved);
	}

	@Override
	@Transactional
	@Caching(evict = {
		@CacheEvict(value = "replyList", key = "#replyId"),
		@CacheEvict(value = "replyPages", allEntries = true),
		@CacheEvict(value = "commentList", allEntries = true),
		@CacheEvict(value = "productDetailList", allEntries = true)
	})
	public void softDeleteById(Long replyId) {
		replyRepo.softDelete(replyId);
	}

	@Override
	@Cacheable(value = "replyList", key = "#replyId")
	public ReplyResponse findById(Long replyId) {
		// TODO Auto-generated method stub
		if (replyId == null) throw new IllegalArgumentException("Can't not find id when reply is null");
		Reply exist = replyRepo.findById(Long.valueOf(replyId))
				.orElseThrow(() -> new EntityNotFoundException("Reply not found with Id: " + replyId));
		return replyMapper.toResponse(exist);
	}

	@Override
	public List<ReplyResponse> findAll() {
		// TODO Auto-generated method stub
		List<Reply> replies = replyRepo.findAll();
		return replyMapper.toResponseList(replies);
	}

	@Override
	@Cacheable(value = "replyPages", key = "#productSearch + '_' + #commentSearch + '_' + #replySearch + '_' + #fromDate + '_' + #toDate + '_' + #accountId + '_' + #commentId + '_' + #deleted + '_' + #sortOrder + '_' + #pageNumber + '_' + #pageSize")
	public PageResponse<ReplyResponse> filterAndPaginateReply(
	        String productSearch,
		String commentSearch,
		String replySearch,
	        LocalDateTime fromDate,
	        LocalDateTime toDate,
			Long accountId,
			Long commentId,
	        Boolean deleted,
			SortOrder sortOrder,
	        Integer pageNumber,
			Integer pageSize) {
		
		Sort sort = sortOrder == SortOrder.ASC
	            ? Sort.by("id").ascending()
	            : Sort.by("id").descending();
		Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
	    Page<Reply> page = replyRepo.filterReplies(productSearch, commentSearch, replySearch, fromDate, toDate, accountId, commentId, deleted, pageable);
	    List<ReplyResponse> responses = replyMapper.toResponseList(page.getContent());
	    return new PageResponse<>(page, responses);
	}

	@Override
	public void warmupCache(Integer times) {
		if (times < 1) return;
		for (int i = 0; i < times; i++) {
			filterAndPaginateReply(null, null, null, null, null, null, null, null, SortOrder.DESC, 0, 5);
		}
	}
}
