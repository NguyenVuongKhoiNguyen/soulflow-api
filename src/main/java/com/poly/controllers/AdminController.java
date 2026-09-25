package com.poly.controllers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpSession;

import com.poly.models.enums.RoleCode;
import com.poly.models.enums.SortOrder;
import com.poly.models.requests.AccountRequest;
import com.poly.models.requests.CategoryRequest;
import com.poly.models.requests.CommentRequest;
import com.poly.models.requests.DiscountRequest;
import com.poly.models.requests.ProductImageRequest;
import com.poly.models.requests.ProductRequest;
import com.poly.models.requests.AdminOrderUpdateRequest;
import com.poly.models.requests.StoreRequest;
import com.poly.models.responses.AccountResponse;
import com.poly.models.responses.CategoryResponse;
import com.poly.models.responses.CommentResponse;
import com.poly.models.responses.DiscountResponse;
import com.poly.models.responses.DashboardPeriodResponse;
import com.poly.models.responses.DashboardCategorySalesResponse;
import com.poly.models.responses.DashboardProductSalesResponse;
import com.poly.models.responses.PageResponse;
import com.poly.models.responses.ProductImageResponse;
import com.poly.models.responses.ProductResponse;
import com.poly.models.responses.NotificationResponse;
import com.poly.models.responses.OrderResponse;
import com.poly.models.responses.StoreResponse;
import com.poly.models.services.BaseService;
import com.poly.models.services.DashboardService;
import com.poly.models.services.NotificationService;
import com.poly.models.services.impl.FilterAsyncService;

import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController extends BaseService {


    private final OrderNotificationWebSocketController orderNotificationWebSocketController;
    private final NotificationService notificationService;
    private final FilterAsyncService filterAsyncService;
    private final DashboardService dashboardService;

    /* notification */

    /* order */

    @PostMapping("/order/{id}")
    OrderResponse updateOrder(@PathVariable Long id, @RequestBody @Valid AdminOrderUpdateRequest request) {
        OrderResponse saved = orderService.updateAdminInfo(id, request);
        orderNotificationWebSocketController.publishUpdated(saved);
        return saved;
    }

    @GetMapping("/notification")
    CompletableFuture<PageResponse<NotificationResponse>> findNotifications(
        @RequestParam(defaultValue = "0") Integer pageNumber,
        @RequestParam(defaultValue = "50") Integer pageSize
    ) {
        return filterAsyncService.run(() -> notificationService.findAll(pageNumber, pageSize));
    }

    @PostMapping("/notification/read-all")
    void markAllNotificationsRead() {
        notificationService.markAllRead();
    }

    @DeleteMapping("/notification")
    void clearNotifications() {
        notificationService.clearAll();
    }

    /* dashboard */

    @GetMapping("/dashboard/daily")
    CompletableFuture<List<DashboardPeriodResponse>> findDailyDashboardMetrics(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return filterAsyncService.run(() -> dashboardService.findDaily(fromDate, toDate));
    }

    @GetMapping("/dashboard/quarterly")
    CompletableFuture<List<DashboardPeriodResponse>> findQuarterlyDashboardMetrics(
        @RequestParam Integer fromYear,
        @RequestParam Integer toYear
    ) {
        return filterAsyncService.run(() -> dashboardService.findQuarterly(fromYear, toYear));
    }

    @GetMapping("/dashboard/yearly")
    CompletableFuture<List<DashboardPeriodResponse>> findYearlyDashboardMetrics(
        @RequestParam Integer fromYear,
        @RequestParam Integer toYear
    ) {
        return filterAsyncService.run(() -> dashboardService.findYearly(fromYear, toYear));
    }

    @GetMapping("/dashboard/daily/categories")
    CompletableFuture<List<DashboardCategorySalesResponse>> findDailyCategorySales(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return filterAsyncService.run(() -> dashboardService.findDailyCategorySales(fromDate, toDate));
    }

    @GetMapping("/dashboard/quarterly/categories")
    CompletableFuture<List<DashboardCategorySalesResponse>> findQuarterlyCategorySales(
        @RequestParam Integer fromYear,
        @RequestParam Integer toYear
    ) {
        return filterAsyncService.run(() -> dashboardService.findQuarterlyCategorySales(fromYear, toYear));
    }

    @GetMapping("/dashboard/yearly/categories")
    CompletableFuture<List<DashboardCategorySalesResponse>> findYearlyCategorySales(
        @RequestParam Integer fromYear,
        @RequestParam Integer toYear
    ) {
        return filterAsyncService.run(() -> dashboardService.findYearlyCategorySales(fromYear, toYear));
    }

    @GetMapping("/dashboard/daily/products")
    CompletableFuture<List<DashboardProductSalesResponse>> findDailyProductSales(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return filterAsyncService.run(() -> dashboardService.findDailyProductSales(fromDate, toDate));
    }

    @GetMapping("/dashboard/quarterly/products")
    CompletableFuture<List<DashboardProductSalesResponse>> findQuarterlyProductSales(
        @RequestParam Integer fromYear,
        @RequestParam Integer toYear
    ) {
        return filterAsyncService.run(() -> dashboardService.findQuarterlyProductSales(fromYear, toYear));
    }

    @GetMapping("/dashboard/yearly/products")
    CompletableFuture<List<DashboardProductSalesResponse>> findYearlyProductSales(
        @RequestParam Integer fromYear,
        @RequestParam Integer toYear
    ) {
        return filterAsyncService.run(() -> dashboardService.findYearlyProductSales(fromYear, toYear));
    }

    /* account */

    @PostMapping("/account")
    AccountResponse saveAccount(
        @RequestPart("account") @Valid AccountRequest request,
        @RequestPart(value = "file", required = false) MultipartFile file) throws Exception {

        if (file != null) {
            String name = imageService.upload(file);
            request.setPhoto(name);
        }

        return accountService.save(request);
    }

    @DeleteMapping("/account/{id}")
    void deleteAccount(@PathVariable Long id) {
        accountService.softDeleteById(id);
    }

    @GetMapping("/account/{id}")
    AccountResponse findAccountById(@PathVariable Long id) {
        return accountService.findById(id);
    }

    @GetMapping("/account/by-username/{username}")
    AccountResponse findAccountByUsername(@PathVariable String username) {
        return accountService.findByUsername(username);
    }

    @GetMapping("/account/by-email/{email}")
    AccountResponse findAccountByEmail(@PathVariable String email) {
        return accountService.findByEmail(email);
    }

    @GetMapping("/account")
    CompletableFuture<PageResponse<AccountResponse>> filterAndPaginateAccounts(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime fromDate,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime toDate,
        @RequestParam(required = false) Boolean deleted,
        @RequestParam(required = false) Boolean disabled,
        @RequestParam(required = false) RoleCode role,
        @RequestParam(defaultValue = "DESC") SortOrder sortOrder,
        @RequestParam(defaultValue = "0") Integer pageNumber,
        @RequestParam(defaultValue = "5") Integer pageSize
        ) {
        return filterAsyncService.run(() -> {
            accountService.checkAndExpireBeforePagination(deleted, keyword, fromDate, toDate, disabled);
            return accountService.filterAndPaginateAccounts(deleted, keyword, fromDate, toDate, disabled, role, sortOrder, pageNumber, pageSize);
        });
    }

    /* category */

    /* store */

    @PostMapping("/store")
    StoreResponse saveStore(@RequestBody @Valid StoreRequest request) {
        return storeService.save(request);
    }
    @PutMapping("/store/{id}/select")
    StoreResponse selectStore(@PathVariable Long id, HttpSession session) {
        StoreResponse store = storeService.findById(id);
        session.setAttribute("selectedStoreId", id);
        return store;
    }

    @DeleteMapping("/store/{id}")
    void deleteStoreById(@PathVariable Long id) {
        storeService.deleteById(id);
    }

    @GetMapping("/store/selected")
    StoreResponse findSelectedStore(HttpSession session) {
        Object selectedStoreId = session.getAttribute("selectedStoreId");
        if (!(selectedStoreId instanceof Long storeId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "No fulfilment store has been selected in this session");
        }
        return storeService.findById(storeId);
    }
    @GetMapping("/store/{id}")
    StoreResponse findStoreById(@PathVariable Long id) {
        return storeService.findById(id);
    }

    @GetMapping("/store")
    CompletableFuture<PageResponse<StoreResponse>> findAllStores(
        @RequestParam(required = false) Long id,
        @RequestParam(required = false) String storeName,
        @RequestParam(defaultValue = "0") Integer pageNumber,
        @RequestParam(defaultValue = "5") Integer pageSize
    ) {
        return filterAsyncService.run(() -> storeService.filterAndPaginateStores(id, storeName, pageNumber, pageSize));
    }

    @PostMapping("/category")
    CategoryResponse save(@RequestBody @Valid CategoryRequest request) {
        return categoryService.save(request);
    }

    @DeleteMapping("/category/{id}")
    void deleteCategoryById(@PathVariable Long id) {
        categoryService.softDeleteById(id);
    }

    @GetMapping("/category/{id}")
    CategoryResponse findCaregoryById(@PathVariable Long id) {
        return categoryService.findById(id);
    }

    @GetMapping("/category")
    CompletableFuture<PageResponse<CategoryResponse>> filterAndPaginateCategories(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Boolean deleted,
        @RequestParam(defaultValue = "0") Integer pageNumber,
        @RequestParam(defaultValue = "DESC") SortOrder sortOrder,
        @RequestParam(defaultValue = "5") Integer pageSize
    ) {
        return filterAsyncService.run(() -> categoryService.filterAndPaginateCategories(keyword, deleted, sortOrder, pageNumber, pageSize));
    }

    /* discount */

    @PostMapping("/discount")
    DiscountResponse save(@RequestBody @Valid DiscountRequest request) {
        return discountService.save(request);
    }

    @DeleteMapping("/discount/{id}")
    void deleteDiscountById(@PathVariable Long id) {
        discountService.softDeleteById(id);
    }

    @GetMapping("/discount/{id}")
    DiscountResponse findDiscountById(@PathVariable Long id) {
        return discountService.findById(id);
    }

    @GetMapping("/discount")
    CompletableFuture<PageResponse<DiscountResponse>> filterAndPaginateDiscounts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime toDate,
            @RequestParam(required = false) Boolean expired,
            @RequestParam(required = false) Boolean deleted,
            @RequestParam(defaultValue = "DESC") SortOrder sortOrder,
            @RequestParam(defaultValue = "0") Integer pageNumber,
            @RequestParam(defaultValue = "5") Integer pageSize
    ) {
        return filterAsyncService.run(() -> {
            discountService.checkAndExpireBeforePagination(keyword, fromDate, toDate, expired, deleted);
            return discountService.filterAndPaginateDiscounts(keyword, fromDate, toDate, expired, deleted, sortOrder, pageNumber, pageSize);
        });
    }

    @PostMapping("/discount/{id}/products")
    DiscountResponse updateDiscountProducts(@PathVariable Long id, @RequestBody UpdateDiscountProductsRequest request) {
        return discountService.updateProducts(id, request.productIds);
    }

    @lombok.Data
    public static class UpdateDiscountProductsRequest {
        private List<Long> productIds;
    }

    /* product */

    @PostMapping("/product")
    ProductResponse save(
            @RequestPart("request") @Valid ProductRequest request,
            @RequestPart(value = "files", required = false) MultipartFile[] files
            ) throws Exception {

        ProductResponse saved = productService.save(request);

        try {
            if (files != null && files.length > 0) {
                for (MultipartFile file : files) {
                    if (!file.isEmpty()) {
                        String name = imageService.upload(file);
                        ProductImageRequest productImageRequest = new ProductImageRequest();
                        productImageRequest.setName(name);
                        productImageRequest.setProductId(Long.valueOf(saved.getId()));
                        productImageService.save(productImageRequest);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return saved;
    }

    @DeleteMapping("/product/{id}")
    void deleteProductById(@PathVariable Long id) {
        productService.softDeleteById(id);
    }

    @PostMapping("/product/add/discount")
    ProductResponse addDiscount(@RequestBody AddDiscountRequest request) {
        return discountService.addDiscounts(request.productId, request.discountIds);
    }

    @lombok.Data
    public static class AddDiscountRequest {
        private Long productId;
        private List<Long> discountIds;
    }

    /* image */

    @PostMapping("/image/delete/{id}")
    void deleteImageById(@PathVariable Long id) {
        productImageService.softDeleteById(id);
    }

    @PostMapping("/image/upload")
    ProductImageResponse uploadImage(
            @RequestParam Long productId,
            @RequestPart("file") MultipartFile file) throws Exception {
        String name = imageService.upload(file);
        ProductImageRequest req = new ProductImageRequest();
        req.setName(name);
        req.setProductId(productId);
        return productImageService.save(req);
    }

    @GetMapping("/image")
    CompletableFuture<PageResponse<ProductImageResponse>> filterAndPaginateProductImages(
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "DESC") SortOrder sortOrder,
        @RequestParam(defaultValue = "0") Integer pageNumber,
        @RequestParam(defaultValue = "5") Integer pageSize
    ) {
        return filterAsyncService.run(() -> productImageService.filterAndPagonateProductImages(keyword, sortOrder, pageNumber, pageSize));
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
}



