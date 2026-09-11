package com.poly.controllers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.poly.models.enums.SortOrder;
import com.poly.models.requests.AuthRequest;
import com.poly.models.responses.AuthResponse;
import com.poly.models.responses.CategoryResponse;
import com.poly.models.responses.CommentResponse;
import com.poly.models.responses.PageResponse;
import com.poly.models.responses.ProductResponse;
import com.poly.models.responses.ReplyResponse;
import com.poly.models.services.BaseService;
import com.poly.models.services.impl.AccountServiceImpl.GoogleTokenDTO;

@RestController
public class NonUserController extends BaseService {

    @PostMapping("/login")
    AuthResponse login(@RequestBody AuthRequest request) {
        return accountService.login(request);
    }

    @PostMapping("/google/login")
    AuthResponse googleLogin(@RequestBody GoogleTokenDTO token) {
        return accountService.loginWithGoogle(token);
    }

    @GetMapping("/category/list")
    List<CategoryResponse> findAll() {
        return categoryService.findAll();
    }

    @GetMapping("/comment")
    PageResponse<CommentResponse> filterAndPaginateComments(
        @RequestParam(required = false) String keyword, 
		@RequestParam(required = false) LocalDateTime fromDate,
		@RequestParam(required = false) LocalDateTime toDate,
        @RequestParam(required = false) Long productId,
        @RequestParam(required = false) Long accountId, 
		@RequestParam(required = false) Boolean deleted, 
		@RequestParam(defaultValue = "DESC") SortOrder sortOrder, 
		@RequestParam(defaultValue = "0") Integer pageNumber, 
		@RequestParam(defaultValue = "5") Integer pageSize
    ) {
        return commentService.filterAndPaginateComments(keyword, fromDate, toDate, productId, accountId, deleted, sortOrder, pageNumber, pageSize);
    }

    @GetMapping("/reply")
    PageResponse<ReplyResponse> filterAndPaginateReplies(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) LocalDateTime fromDate,
            @RequestParam(required = false) LocalDateTime toDate,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) Long commentId,
            @RequestParam(required = false) Boolean deleted,
            @RequestParam(defaultValue = "DESC") SortOrder sortOrder,
            @RequestParam(defaultValue = "0") Integer pageNumber,
            @RequestParam(defaultValue = "5") Integer pageSize
    ) {
        return replyService.filterAndPaginateReply(keyword, fromDate, toDate, accountId, commentId, deleted, sortOrder, pageNumber, pageSize);
    }

    @GetMapping("/product/{id}")
	ProductResponse findProductById(@PathVariable Long id) {
		return productService.findById(id);
	}

    @GetMapping("/product")
	PageResponse<ProductResponse> filterAndPaginateProducts(
			@RequestParam(required = false) String keyword, 
			@RequestParam(required = false) BigDecimal minPrice, 
			@RequestParam(required = false) BigDecimal maxPrice, 
			@RequestParam(required = false) LocalDateTime fromDate,
			@RequestParam(required = false) LocalDateTime toDate,
			@RequestParam(required = false) List<Long> categoryIds, 
            @RequestParam(required = false) Boolean customised,
			@RequestParam(required = false) Boolean available,
			@RequestParam(required = false) Boolean deleted,
			@RequestParam(defaultValue = "DESC") SortOrder sortOrder, 
			@RequestParam(defaultValue = "0") Integer pageNumber, 
			@RequestParam(defaultValue = "5") Integer pageSize
	) {
		return productService.filterAndPaginateProducts(keyword, minPrice, maxPrice, categoryIds, customised, available, deleted, fromDate, toDate, sortOrder, pageNumber, pageSize);
	}

}
