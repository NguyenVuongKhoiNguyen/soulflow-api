package com.poly.models.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.poly.models.entities.Category;
import com.poly.models.entities.Discount;
import com.poly.models.entities.Product;
import com.poly.models.repositories.CategoryRepository;
import com.poly.models.repositories.DiscountRepository;
import com.poly.models.repositories.ProductRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShopDataTools {

    private static final int MAX_RESULTS = 30;

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final DiscountRepository discountRepository;

    @Tool(description = """
        Search the live flower-shop catalog. Use this before answering questions about products,
        recommendations, prices, stock, categories, or customisable bouquets. All parameters are
        optional. Results contain current database values and are limited to available stock.
        """)
    public List<ProductKnowledge> searchCatalog(
            @ToolParam(description = "Product ID, name, flower type, occasion, or description keyword", required = false)
            String keyword,
            @ToolParam(description = "Category ID or category name", required = false)
            String category,
            @ToolParam(description = "Minimum original product price", required = false)
            BigDecimal minPrice,
            @ToolParam(description = "Maximum original product price", required = false)
            BigDecimal maxPrice,
            @ToolParam(description = "Whether the product must support customisation", required = false)
            Boolean customised) {

        return productRepository.searchAvailableForAi(
                normalise(keyword),
                normalise(category),
                minPrice,
                maxPrice,
                customised,
                PageRequest.of(0, MAX_RESULTS))
            .stream()
            .map(this::toKnowledge)
            .toList();
    }

    @Tool(description = "Get full live catalog details for one product using its exact product ID.")
    public ProductKnowledge getProductById(
            @ToolParam(description = "Exact numeric product ID shown in the catalog") Long id) {
        if (id == null) {
            return null;
        }
        return productRepository.findActiveByIdForAi(id)
            .map(this::toKnowledge)
            .orElse(null);
    }

    @Tool(description = "List all current, non-deleted product categories from the shop database.")
    public List<CategoryKnowledge> listCategories() {
        return categoryRepository.findByDeletedFalseOrderByNameAsc()
            .stream()
            .map(this::toKnowledge)
            .toList();
    }

    @Tool(description = "List all currently active product discounts and how many products currently use each discount.")
    public List<DiscountKnowledge> listActiveDiscounts() {
        return discountRepository.findActiveForAi()
            .stream()
            .map(this::toKnowledge)
            .toList();
    }

    private ProductKnowledge toKnowledge(Product product) {
        Category category = product.getCategory();
        return new ProductKnowledge(
            product.getId(),
            product.getName(),
            product.getDescription(),
            category == null ? null : category.getId(),
            category == null ? null : category.getName(),
            product.getPrice(),
            product.getQuantity(),
            Boolean.TRUE.equals(product.getAvailable()),
            Boolean.TRUE.equals(product.getCustomised()),
            product.getSales()
        );
    }

    private CategoryKnowledge toKnowledge(Category category) {
        return new CategoryKnowledge(
            category.getId(),
            category.getName(),
            category.getDescription()
        );
    }

    private DiscountKnowledge toKnowledge(Discount discount) {
        return new DiscountKnowledge(
            discount.getCode(),
            discount.getPercentage(),
            discount.getDescription(),
            discount.getExpiredDate(),
            discount.getProducts().size()
        );
    }

    private boolean isActive(Discount discount) {
        return !Boolean.TRUE.equals(discount.getDeleted())
            && !Boolean.TRUE.equals(discount.getExpired())
            && (discount.getExpiredDate() == null || discount.getExpiredDate().isAfter(LocalDateTime.now()));
    }

    private String normalise(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record ProductKnowledge(
        Long id,
        String name,
        String description,
        Long categoryId,
        String categoryName,
        BigDecimal originalPrice,
        Integer quantityInStock,
        boolean available,
        boolean customisable,
        Long sales
    ) {}

    public record CategoryKnowledge(
        Long id,
        String name,
        String description
    ) {}

    public record DiscountKnowledge(
        String code,
        BigDecimal percentage,
        String description,
        LocalDateTime expiresAt,
        int assignedProductCount
    ) {}
}
