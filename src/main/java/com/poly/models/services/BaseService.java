package com.poly.models.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public abstract class BaseService {

    @Autowired
    public ImageService imageService;

    @Autowired
    public AccountService accountService;

    @Autowired
    public CategoryService categoryService;

    @Autowired
    public ProductService productService;

    @Autowired
    public CartService cartService;

    @Autowired
    public CommentService commentService;

    @Autowired
    public OrderService orderService;

    @Autowired
    public ReplyService replyService;

    @Autowired
    public ProductImageService productImageService;

    @Autowired
    public DiscountService discountService;

    @Autowired
    public PaymentService paymentService;

    @Autowired
    public QrService qrService;

    @Autowired
    public StoreService storeService;

}
