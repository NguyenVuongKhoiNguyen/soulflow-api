package com.poly.controllers;

import java.time.LocalDateTime;
import java.security.Principal;
import java.util.concurrent.CompletableFuture;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;

import com.poly.models.enums.OrderStatus;
import com.poly.models.enums.SortOrder;
import com.poly.models.requests.CartRequest;
import com.poly.models.requests.CommentRequest;
import com.poly.models.requests.OrderRequest;
import com.poly.models.requests.PaymentRequest;
import com.poly.models.requests.ReplyRequest;
import com.poly.models.requests.AccountRequest;
import com.poly.models.responses.AccountResponse;
import com.poly.models.responses.CartResponse;
import com.poly.models.responses.CommentResponse;
import com.poly.models.responses.OrderResponse;
import com.poly.models.responses.PageResponse;
import com.poly.models.responses.ReplyResponse;
import com.poly.models.services.BaseService;
import com.poly.models.services.impl.FilterAsyncService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController extends BaseService {

    private final FilterAsyncService filterAsyncService;
    private final OrderNotificationWebSocketController orderNotificationWebSocketController;

    /* account */

    @PostMapping("/account/update")
    AccountResponse updateProfile(
        @RequestPart("account") @Valid AccountRequest request,
        @RequestPart(value = "file", required = false) MultipartFile file,
        Principal principal
    ) throws Exception {
        AccountResponse current = accountService.findByUsername(principal.getName());
        request.setId(Long.valueOf(current.getId()));
        request.setUsername(current.getUsername());
        request.setEmail(current.getEmail());
        request.setRoleRequests(null);
        request.setDisabled(null);
        
        if (file != null) {
            String name = imageService.upload(file);
            request.setPhoto(name);
        }
        
        return accountService.save(request);
    }

    /* cart */

    @PostMapping("/cart")
    CartResponse saveCart(@RequestBody @Valid CartRequest request) {
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
    CompletableFuture<PageResponse<CartResponse>> filterAndPaginateCarts(
        @RequestParam(required = false) Long accountId,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) LocalDateTime fromDate,
        @RequestParam(required = false) LocalDateTime toDate,
        @RequestParam(required = false) Boolean expired,
        @RequestParam(required = false) Boolean deleted,
        @RequestParam(defaultValue = "DESC") SortOrder sortOrder,
        @RequestParam(defaultValue = "0") Integer pageNumber,
        @RequestParam(defaultValue = "5") Integer pageSize
    ) {
        return filterAsyncService.run(() -> {
            cartService.checkAndExpireBeforePagination(keyword, fromDate, toDate, expired, deleted);
            return cartService.filterAndPaginateCarts(accountId, keyword, fromDate, toDate, expired, deleted, sortOrder, pageNumber, pageSize);
        });
    }


    /* comment */

    @PostMapping("/comment")
    CommentResponse saveComment(@RequestBody @Valid CommentRequest request) {
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

	@GetMapping("/order/mine")
	CompletableFuture<PageResponse<OrderResponse>> findMyOrders(
		Principal principal,
		@RequestParam(defaultValue = "0") Integer pageNumber,
		@RequestParam(defaultValue = "8") Integer pageSize
	) {
		String username = principal.getName();
		return filterAsyncService.run(() -> orderService.findMine(username, pageNumber, pageSize));
	}

    @GetMapping("/order/mine/{id}")
    OrderResponse findMyOrderById(Principal principal, @PathVariable Long id) {
        return orderService.findMineById(principal.getName(), id);
    }

	@PostMapping("/order")
	OrderResponse saveOrder(@RequestBody @Valid OrderRequest request) {
        boolean creating = request.getId() == null;
        OrderResponse saved = orderService.save(request);
        if (creating) {
            orderNotificationWebSocketController.publishCreated(saved);
        } else {
            orderNotificationWebSocketController.publishUpdated(saved);
        }
        return saved;
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
	CompletableFuture<PageResponse<OrderResponse>> filterAndPaginateOrders(
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
        return filterAsyncService.run(() -> {
            orderService.checkAndExpireBeforePagination(keyword, fromDate, toDate, status, expired, deleted);
            return orderService.filterAndPaginateOrders(keyword, fromDate, toDate, status, expired, deleted, sortOrder, pageNumber, pageSize);
        });
    }

    /* payment */

    @PostMapping("/payment")
    OrderResponse savePayment(@RequestBody @Valid PaymentRequest request) {
        paymentService.save(request);
        Long orderId = Long.valueOf(request.getOrderId());
        orderService.markOrderAsPaidIfFullyPaid(orderId);
        return orderService.findById(orderId);
    }

    /* reply */

    @PostMapping("/reply")
    ReplyResponse saveReply(@RequestBody @Valid ReplyRequest request) {
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
    CompletableFuture<PageResponse<ReplyResponse>> filterAndpaginateReplies(
        @RequestParam(required = false) String productSearch,
        @RequestParam(required = false) String commentSearch,
        @RequestParam(required = false) String replySearch,
            @RequestParam(required = false) LocalDateTime fromDate,
            @RequestParam(required = false) LocalDateTime toDate,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) Long commentId,
            @RequestParam(required = false) Boolean deleted,
            @RequestParam(defaultValue = "DESC") SortOrder sortOrder,
            @RequestParam(defaultValue = "0") Integer pageNumber,
            @RequestParam(defaultValue = "5") Integer pageSize
    ) {
        return filterAsyncService.run(() -> replyService.filterAndPaginateReply(productSearch, commentSearch, replySearch, fromDate, toDate, accountId, commentId, deleted, sortOrder, pageNumber, pageSize));
    }
}
