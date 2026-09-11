package com.poly.config;

import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import com.poly.models.entities.Category;
import com.poly.models.entities.Product;
import com.poly.models.entities.ProductImage;
import com.poly.models.repositories.CategoryRepository;
import com.poly.models.repositories.ProductImageRepository;
import com.poly.models.repositories.ProductRepository;
import com.poly.models.services.ImageService;

import lombok.RequiredArgsConstructor;

@Component
@Order(2)
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "app.catalog-seeding.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class CatalogDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CatalogDataSeeder.class);
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final Map<String, String> CATEGORY_NAMES = Map.of(
        "Bouquest", "Bouquet",
        "Flower_Basket", "Flower Basket",
        "Table_Plant", "Table Plant"
    );

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ImageService imageService;

    @Value("${app.catalog-seeding.default-price:100000}")
    private BigDecimal defaultPrice;

    @Value("${app.catalog-seeding.default-quantity:10}")
    private Integer defaultQuantity;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        List<CatalogImage> images = discoverImages();
        int createdProducts = 0;
        int createdImages = 0;
        int uploadedObjects = 0;

        for (CatalogImage image : images) {
            Category category = findOrCreateCategory(image.categoryFolder());
            ProductSeedResult productResult = findOrCreateProduct(category, image.productName());
            Product product = productResult.product();
            if (productResult.created()) {
                createdProducts++;
            }

            boolean imageRowExists = productImageRepository.existsByProductIdAndName(
                product.getId(), image.objectName()
            );
            boolean objectExists = imageService.exists(image.objectName());

            if (!objectExists) {
                try (InputStream stream = image.resource().getInputStream()) {
                    imageService.upload(
                        stream,
                        image.objectName(),
                        contentType(image.fileName()),
                        image.resource().contentLength()
                    );
                }
                uploadedObjects++;
            }

            if (!imageRowExists) {
                ProductImage productImage = new ProductImage();
                productImage.setName(image.objectName());
                productImage.setCreatedDate(LocalDateTime.now());
                productImage.setDeleted(false);
                productImage.setProduct(product);
                productImageRepository.save(productImage);
                createdImages++;
            }
        }

        log.info(
            "Catalog seed complete: {} source images, {} products created, {} image rows created, {} MinIO objects uploaded",
            images.size(), createdProducts, createdImages, uploadedObjects
        );
    }

    private List<CatalogImage> discoverImages() throws Exception {
        Resource[] resources = new PathMatchingResourcePatternResolver()
            .getResources("classpath*:images/*/*/*");
        List<CatalogImage> images = new ArrayList<>();

        for (Resource resource : resources) {
            String fileName = resource.getFilename();
            if (fileName == null || !SUPPORTED_EXTENSIONS.contains(extension(fileName))) {
                continue;
            }

            String location = URLDecoder.decode(resource.getURL().toExternalForm(), StandardCharsets.UTF_8);
            String normalized = location.replace('\\', '/');
            int imageRoot = normalized.lastIndexOf("/images/");
            if (imageRoot < 0) {
                log.warn("Skipping catalog resource with an unexpected path: {}", location);
                continue;
            }

            String[] parts = normalized.substring(imageRoot + "/images/".length()).split("/");
            if (parts.length != 3) {
                log.warn("Skipping catalog resource outside category/product/image hierarchy: {}", location);
                continue;
            }

            String objectName = "catalog/" + parts[0] + "/" + parts[1] + "/" + parts[2];
            images.add(new CatalogImage(parts[0], parts[1], parts[2], objectName, resource));
        }

        images.sort(Comparator.comparing(CatalogImage::objectName));
        return images;
    }

    private Category findOrCreateCategory(String folderName) {
        String categoryName = CATEGORY_NAMES.getOrDefault(folderName, folderName.replace('_', ' '));
        return categoryRepository.findFirstByNameIgnoreCase(categoryName)
            .map(category -> {
                if (Boolean.TRUE.equals(category.getDeleted())) {
                    category.setDeleted(false);
                    return categoryRepository.save(category);
                }
                return category;
            })
            .orElseGet(() -> {
                Category category = new Category();
                category.setName(categoryName);
                category.setDescription("Products imported from the " + categoryName + " image catalog");
                category.setDeleted(false);
                return categoryRepository.save(category);
            });
    }

    private ProductSeedResult findOrCreateProduct(Category category, String productName) {
        return productRepository.findFirstByCategoryIdAndNameIgnoreCase(category.getId(), productName)
            .map(product -> {
                if (Boolean.TRUE.equals(product.getDeleted())) {
                    product.setDeleted(false);
                    productRepository.save(product);
                }
                return new ProductSeedResult(product, false);
            })
            .orElseGet(() -> {
                Product product = new Product();
                product.setName(productName);
                product.setDescription(productName + " from the " + category.getName() + " collection");
                product.setPrice(defaultPrice);
                product.setCreatedDate(LocalDateTime.now());
                product.setAvailable(true);
                product.setQuantity(defaultQuantity);
                product.setCustomised(false);
                product.setSales(0L);
                product.setDeleted(false);
                product.setCategory(category);
                return new ProductSeedResult(productRepository.save(product), true);
            });
    }

    private String contentType(String fileName) {
        return switch (extension(fileName)) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            case "gif" -> "image/gif";
            default -> "application/octet-stream";
        };
    }

    private String extension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private record CatalogImage(
        String categoryFolder,
        String productName,
        String fileName,
        String objectName,
        Resource resource
    ) {
    }

    private record ProductSeedResult(Product product, boolean created) {
    }
}
