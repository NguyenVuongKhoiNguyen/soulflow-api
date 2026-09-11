package com.poly.models.services.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.poly.models.entities.Comment;
import com.poly.models.enums.SortOrder;
import com.poly.models.mappers.CommentMapper;
import com.poly.models.repositories.CommentRepository;
import com.poly.models.requests.CommentRequest;
import com.poly.models.responses.CommentResponse;
import com.poly.models.responses.PageResponse;
import com.poly.models.services.CommentService;

import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

	private final CommentRepository commentRepo;
	private final CommentMapper commentMapper;

	@Override
	@Transactional
	@CachePut(value = "commentList", key = "#result.id")
    @CacheEvict(value = "commentPages", allEntries = true)
	public CommentResponse save(CommentRequest request) {
		Comment comment = commentMapper.toEntity(request);
		Comment saved = commentRepo.save(comment);
		return commentMapper.toBasicResponse(saved);
	}

	@Override
	@Transactional
	    @Caching(evict = {
    	@CacheEvict(value = "commentList", key = "#commentId"),
    	@CacheEvict(value = "commentPages", allEntries = true)
    })
	public void softDeleteById(Long commentId) {
		// TODO Auto-generated method stub
		commentRepo.softDelete(commentId);
	}

	@Override
	@Cacheable(value = "commentList", key = "#commentId")
	public CommentResponse findById(Long commentId) {
		// TODO Auto-generated method stub
		if (commentId == null ) throw new IllegalArgumentException("Can't not find comment when id is null");
		Comment exist = commentRepo.findById(Long.valueOf(commentId))
				.orElseThrow(() -> new EntityNotFoundException("Comment not found with Id: " + commentId));
		return commentMapper.toDetailedResponse(exist);
	}
 
	@Override
	@Cacheable(value = "commentPages", key = "#keyword + '_' + #fromDate + '_' + #toDate + '_' + #productId + '_' + #accountId + '_' + #deleted + '_' + #sortOrder + '_' + #pageNumber + '_' + #pageSize")
	public PageResponse<CommentResponse> filterAndPaginateComments(
		String keyword,
		LocalDateTime fromDate,
		LocalDateTime toDate, 
		Long productId, 
		Long accountId, 
		Boolean deleted, 
		SortOrder sortOrder, 
		Integer pageNumber, 
		Integer pageSize
	) {
		// TODO Auto-generated method stub
		Sort sort = sortOrder == SortOrder.ASC
	            ? Sort.by("id").ascending()
	            : Sort.by("id").descending();
		Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
		Page<Comment> page = commentRepo.filterComments(keyword, fromDate, toDate, productId, accountId, deleted, pageable);
		List<CommentResponse> responses = commentMapper.toBasicResponseList(page.getContent());
		return new PageResponse<>(page, responses);
	}

	@Override
	public void warmupCache(Integer times) {
		if (times < 1) return;
		for (int i = 0; i < times; i++) {
			filterAndPaginateComments(null, null, null, null, null, null, SortOrder.DESC, 0, 5);	
		}
	}
}
