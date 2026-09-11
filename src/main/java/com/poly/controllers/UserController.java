package com.poly.controllers;

import java.time.LocalDateTime;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.poly.models.enums.OrderStatus;
import com.poly.models.enums.SortOrder;
import com.poly.models.requests.CartRequest;
import com.poly.models.requests.CommentRequest;
import com.poly.models.requests.OrderRequest;
import com.poly.models.requests.PaymentRequest;
import com.poly.models.requests.ReplyRequest;
import com.poly.models.responses.CartResponse;
import com.poly.models.responses.CommentResponse;
import com.poly.models.responses.OrderResponse;
import com.poly.models.responses.PageResponse;
import com.poly.models.responses.ReplyResponse;
import com.poly.models.services.BaseService;

@RestController
@RequestMapping("/user")
public class UserController extends BaseService {

    /* cart */

    @PostMapping("/cart")
    CartResponse saveCart(@RequestBody CartRequest request) {
        return cartService.save(request);
    }

    @DeleteMapping("/cart/{id}")
    void deleteCartById(@PathVariable Long id) {
        cartService.softDeleteById(id);
    }

    @GetMapping("/cart/{id}")
    CartResponse findCartById(@PathVariable Long id) {
        return cartService.findById(id);
    }

    @GetMapping("/cart")
    PageResponse<CartResponse> filterAndPaginateCarts(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) LocalDateTime fromDate,
        @RequestParam(required = false) LocalDateTime toDate,
        @RequestParam(required = false) Boolean expired,
        @RequestParam(required = false) Boolean deleted,
        @RequestParam(defaultValue = "DESC") SortOrder sortOrder,
        @RequestParam(defaultValue = "0") Integer pageNumber,
        @RequestParam(defaultValue = "5") Integer pageSize
    ) {
        cartService.checkAndExpireBeforePagination(keyword, fromDate, toDate, expired, deleted);
        return cartService.filterAndPaginateCarts(keyword, fromDate, toDate, expired, deleted, sortOrder, pageNumber, pageSize);
    }


    /* comment */

    @PostMapping("/comment")
    CommentResponse saveComment(@RequestBody CommentRequest request) {
        return commentService.save(request);
    }

    @DeleteMapping("/comment/{id}")
    void deleteCommentById(@PathVariable Long id) {
        commentService.softDeleteById(id);
    }

    @GetMapping("/comment/{id}")
    CommentResponse findCommentById(@PathVariable Long id) {
        return commentService.findById(id);
    }

    /* order */

    @PostMapping("/order")
	OrderResponse saveOrder(@RequestBody OrderRequest request) {
        return orderService.save(request);
    }
	
	@DeleteMapping("/order/{id}")
	void deleteOrderById(@PathVariable Long id) {
        orderService.softDeleteById(id);
    }

	@GetMapping("/order/{id}")
	OrderResponse findOrderById(@PathVariable Long id) {
        return orderService.findById(id);
    }
	
	@GetMapping("/order")
	PageResponse<OrderResponse> filterAndPaginateOrders(
			@RequestParam(required = false) String keyword,
            @RequestParam(required = false) LocalDateTime fromDate,
            @RequestParam(required = false) LocalDateTime toDate,
            @RequestParam(required = false) Boolean expired,
            @RequestParam(required = false) Boolean deleted,
			@RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "DESC") SortOrder sortOrder,
            @RequestParam(defaultValue = "0") Integer pageNumber,
            @RequestParam(defaultValue = "5") Integer pageSize
	) {
        orderService.checkAndExpireBeforePagination(keyword, fromDate, toDate, status, expired, deleted);
        return orderService.filterAndPaginateOrders(keyword, fromDate, toDate, status, expired, deleted, sortOrder, pageNumber, pageSize);
    }

    /* payment */

    @PostMapping("/payment")
    OrderResponse savePayment(@RequestBody PaymentRequest request) {
        paymentService.save(request);
        Long orderId = Long.valueOf(request.getOrderId());
        orderService.markOrderAsPaidIfFullyPaid(orderId);
        return orderService.findById(orderId);
    }

    /* reply */

    @PostMapping("/reply")
    ReplyResponse saveReply(@RequestBody ReplyRequest request) {
        return replyService.save(request);
    }

    @DeleteMapping("/reply/{id}")
    void deleteReplyById(@PathVariable Long id) {
        replyService.softDeleteById(id);
    }

    @GetMapping("/reply/{id}")
    ReplyResponse findReplyById(@PathVariable Long id) {
        return replyService.findById(id);
    }

    @GetMapping("/reply")
    PageResponse<ReplyResponse> filterAndpaginateReplies(
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
}
