package com.poly.controllers;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
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

import com.poly.models.enums.RoleCode;
import com.poly.models.enums.SortOrder;
import com.poly.models.requests.AccountRequest;
import com.poly.models.requests.CategoryRequest;
import com.poly.models.requests.DiscountRequest;
import com.poly.models.requests.ProductImageRequest;
import com.poly.models.requests.ProductRequest;
import com.poly.models.responses.AccountResponse;
import com.poly.models.responses.CategoryResponse;
import com.poly.models.responses.CommentResponse;
import com.poly.models.responses.DiscountResponse;
import com.poly.models.responses.PageResponse;
import com.poly.models.responses.ProductImageResponse;
import com.poly.models.responses.ProductResponse;
import com.poly.models.services.BaseService;


@RestController
@RequestMapping("/admin")
public class AdminController extends BaseService {

    /* account */

    @PostMapping("/account")
    AccountResponse save(
        @RequestPart("account") AccountRequest request,
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
    PageResponse<AccountResponse> filterAndPaginateAccounts(
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
        return accountService.filterAndPaginateAccounts(deleted, keyword, fromDate, toDate, disabled, role, sortOrder, pageNumber, pageSize);
    }

    /* category */

    @PostMapping("/category")
    CategoryResponse save(@RequestBody CategoryRequest request) {
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
    PageResponse<CategoryResponse> filterAndPaginateCategories(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Boolean deleted,
        @RequestParam(defaultValue = "0") Integer pageNumber,
        @RequestParam(defaultValue = "DESC") SortOrder sortOrder,
        @RequestParam(defaultValue = "5") Integer pageSize
    ) {
        return categoryService.filterAndPaginateCategories(keyword, deleted, sortOrder, pageNumber, pageSize);
    }

    /* discount */

    @PostMapping("/discount")
    DiscountResponse save(@RequestBody DiscountRequest request) {
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
    PageResponse<DiscountResponse> filterAndPaginateDiscounts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime toDate,
            @RequestParam(required = false) Boolean expired,
            @RequestParam(required = false) Boolean deleted,
            @RequestParam(defaultValue = "DESC") SortOrder sortOrder,
            @RequestParam(defaultValue = "0") Integer pageNumber,
            @RequestParam(defaultValue = "5") Integer pageSize
    ) {
        discountService.checkAndExpireBeforePagination(keyword, fromDate, toDate, expired, deleted);
        return discountService.filterAndPaginateDiscounts(keyword, fromDate, toDate, expired, deleted, sortOrder, pageNumber, pageSize);
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
            @RequestPart("request") ProductRequest request,
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
    PageResponse<ProductImageResponse> filterAndPaginateProductImages(
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "DESC") SortOrder sortOrder,
        @RequestParam(defaultValue = "0") Integer pageNumber,
        @RequestParam(defaultValue = "5") Integer pageSize
    ) {
        return productImageService.filterAndPagonateProductImages(keyword, sortOrder, pageNumber, pageSize);
    }

    /* comment */

    @PostMapping("/comment")
    CommentResponse saveComment(@RequestBody com.poly.models.requests.CommentRequest request) {
        return commentService.save(request);
    }

    @DeleteMapping("/comment/{id}")
    void deleteCommentById(@PathVariable Long id) {
        commentService.softDeleteById(id);
    }
}
