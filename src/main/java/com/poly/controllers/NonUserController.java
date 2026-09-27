package com.poly.controllers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import com.poly.models.enums.SortOrder;
import com.poly.models.requests.AuthRequest;
import com.poly.models.requests.OrderRequest;
import com.poly.models.requests.SepayWebhookRequest;
import com.poly.models.responses.AccountResponse;
import com.poly.models.responses.AuthResponse;
import com.poly.models.responses.CategoryResponse;
import com.poly.models.responses.CommentResponse;
import com.poly.models.responses.OrderResponse;
import com.poly.models.responses.PageResponse;
import com.poly.models.responses.ProductResponse;
import com.poly.models.responses.ReplyResponse;
import com.poly.models.responses.StoreResponse;
import com.poly.models.services.BaseService;
import com.poly.models.services.impl.AccountServiceImpl.GoogleTokenDTO;
import com.poly.models.services.impl.SepayWebhookService;
import com.poly.models.services.impl.FilterAsyncService;

@RestController
@RequiredArgsConstructor 
public class NonUserController extends BaseService {

    private final FilterAsyncService filterAsyncService;

    @Autowired 
    private com.poly.models.services.impl.RefreshTokenService refreshTokens;

    @Value("${app.auth.cookie.secure:false}")
    private boolean secureCookie;

    private final OrderNotificationWebSocketController orderNotificationWebSocketController;

    private final SepayWebhookService sepayWebhookService;

    @PostMapping("/webhook/sepay")
    public ResponseEntity<Map<String, Boolean>> sepayWebhook(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody SepayWebhookRequest request) {
        sepayWebhookService.authenticate(authorization);
        sepayWebhookService.receive(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(Map.of("success", true));
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

    // The /admin/** rule in SecurityConfig verifies the JWT and ADMIN authority.
    @GetMapping("/admin/auth/check")
    public ResponseEntity<Void> checkAdminAccess() {
        return ResponseEntity.noContent()
            .header(HttpHeaders.CACHE_CONTROL, "no-store")
            .build();
    }

    @PostMapping("/login")
    ResponseEntity<AccountResponse> login(@RequestBody @Valid AuthRequest request, HttpServletRequest servletRequest) {

        //login and generate jwt token
        AuthResponse auth = accountService.login(request);
        //build cookie
        ResponseCookie authCookie = ResponseCookie.from("account_token", auth.getToken())
                .httpOnly(true)
                .secure(useSecureCookie(servletRequest))
                .sameSite("Lax")
                .path("/")
                .maxAge(auth.isRememberMe() ? auth.getMaxAge() : -1)
                .build();

        //build response with cookie and send it
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authCookie.toString(), refreshCookie(auth, servletRequest).toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(auth.getAccountResponse());
    }

    @PostMapping("/google/login")
    ResponseEntity<AccountResponse> googleLogin(@RequestBody GoogleTokenDTO token, HttpServletRequest request) {
        AuthResponse auth = accountService.loginWithGoogle(token);
        ResponseCookie authCookie = ResponseCookie.from("account_token", auth.getToken())
                .httpOnly(true)
                .secure(useSecureCookie(request))
                .sameSite("Lax")
                .path("/")
                .maxAge(auth.isRememberMe() ? auth.getMaxAge() : -1)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authCookie.toString(), refreshCookie(auth, request).toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(auth.getAccountResponse());
    }

    @PostMapping("/auth/logout")
    ResponseEntity<Void> logout(HttpServletRequest request) {
        refreshTokens.revoke(readRefreshToken(request));
        ResponseCookie cookie = ResponseCookie.from("account_token", "")
                .httpOnly(true)
                .secure(useSecureCookie(request))
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie.toString(),
                    ResponseCookie.from("refresh_token", "").httpOnly(true)
                        .secure(useSecureCookie(request)).sameSite("Lax")
                        .path("/").maxAge(0).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .build();
    }

    @PostMapping("/auth/refresh")
    ResponseEntity<AccountResponse> refresh(HttpServletRequest request) {
        AuthResponse auth = accountService.refresh(readRefreshToken(request));
        ResponseCookie accessCookie = ResponseCookie.from("account_token", auth.getToken())
                .httpOnly(true).secure(useSecureCookie(request))
                .sameSite("Lax").path("/").maxAge(auth.isRememberMe() ? auth.getMaxAge() : -1).build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString(), refreshCookie(auth, request).toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(auth.getAccountResponse());
    }

    private ResponseCookie refreshCookie(AuthResponse auth, HttpServletRequest request) {
        return ResponseCookie.from("refresh_token", auth.getRefreshToken())
                .httpOnly(true).secure(useSecureCookie(request))
                .sameSite("Lax").path("/").maxAge(auth.isRememberMe() ? auth.getRefreshMaxAge() : -1).build();
    }

    private boolean useSecureCookie(HttpServletRequest request) {
        return secureCookie || request.isSecure();
    }

    private String readRefreshToken(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("refresh_token".equals(cookie.getName())) return cookie.getValue();
            }
        }
        return null;
    }

    @GetMapping("/category/list")
    List<CategoryResponse> findAll() {
        return categoryService.findAll();
    }

    @GetMapping("/store")
    List<StoreResponse> findAllStores() {
        return storeService.findAll();
    }

    @GetMapping("/store/{id}")
    StoreResponse findStoreById(@PathVariable Long id) {
        return storeService.findById(id);
    }

    @GetMapping("/comment")
    CompletableFuture<PageResponse<CommentResponse>> filterAndPaginateComments(
        @RequestParam(required = false) String productSearch,
        @RequestParam(required = false) String commentSearch, 
		@RequestParam(required = false) LocalDateTime fromDate,
		@RequestParam(required = false) LocalDateTime toDate,
        @RequestParam(required = false) Long productId,
        @RequestParam(required = false) Long accountId, 
		@RequestParam(required = false) Boolean deleted, 
		@RequestParam(defaultValue = "DESC") SortOrder sortOrder, 
		@RequestParam(defaultValue = "0") Integer pageNumber, 
		@RequestParam(defaultValue = "5") Integer pageSize
    ) {
        return filterAsyncService.run(() -> commentService.filterAndPaginateComments(productSearch, commentSearch, fromDate, toDate, productId, accountId, deleted, sortOrder, pageNumber, pageSize));
    }

    @GetMapping("/reply")
    CompletableFuture<PageResponse<ReplyResponse>> filterAndPaginateReplies(
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

    @GetMapping("/product/{id}")
	ProductResponse findProductById(@PathVariable Long id) {
		return productService.findById(id);
	}

    @GetMapping("/product")
	CompletableFuture<PageResponse<ProductResponse>> filterAndPaginateProducts(
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
		return filterAsyncService.run(() -> productService.filterAndPaginateProducts(keyword, minPrice, maxPrice, categoryIds, customised, available, deleted, fromDate, toDate, sortOrder, pageNumber, pageSize));
	}

}


