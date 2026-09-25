CREATE TABLE categories (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(100) NOT NULL,
    description NVARCHAR(255),
    del_if BIT NOT NULL DEFAULT 0
);

CREATE TABLE roles (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    code VARCHAR(10) NOT NULL UNIQUE,
    name NVARCHAR(100) NOT NULL UNIQUE,
    del_if BIT NOT NULL DEFAULT 0
);

CREATE TABLE accounts (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    fullname NVARCHAR(100),
    email VARCHAR(100) NOT NULL UNIQUE,
    photo NVARCHAR(255),
    address NVARCHAR(255),
    phone_number VARCHAR(15),
    created_date DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    disabled BIT NOT NULL DEFAULT 0,
    credential_expired_date DATETIME2,
    credential_expired BIT NOT NULL DEFAULT 0,
    del_if BIT NOT NULL DEFAULT 0
);

CREATE TABLE account_role (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    account_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    CONSTRAINT fk_account_role_accounts FOREIGN KEY (account_id) REFERENCES accounts(id),
    CONSTRAINT fk_account_role_roles FOREIGN KEY (role_id) REFERENCES roles(id),
    CONSTRAINT uq_account_role UNIQUE (account_id, role_id)
);

CREATE TABLE products (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(100) NOT NULL,
    description NVARCHAR(500),
    price DECIMAL(18,2) NOT NULL,
    created_date DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    available BIT NOT NULL DEFAULT 1,
    quantity INT NOT NULL DEFAULT 5,
    customised BIT NOT NULL DEFAULT 0,
    sales BIGINT NOT NULL DEFAULT 0,
    del_if BIT NOT NULL DEFAULT 0,
    category_id BIGINT NOT NULL,
    CONSTRAINT ck_products_price CHECK (price >= 0),
    CONSTRAINT ck_products_quantity CHECK (quantity >= 0),
    CONSTRAINT ck_products_sales CHECK (sales >= 0),
    CONSTRAINT fk_products_categories FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE TABLE product_images (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(500) NOT NULL,
    created_date DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    del_if BIT NOT NULL DEFAULT 0,
    product_id BIGINT NOT NULL,
    CONSTRAINT fk_product_images_products FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE stores (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(100) NOT NULL,
    phone VARCHAR(15) NOT NULL,
    address NVARCHAR(255) NOT NULL
);

CREATE TABLE orders (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    fullname NVARCHAR(100) NOT NULL,
    phone_number VARCHAR(15) NOT NULL,
    address NVARCHAR(255) NOT NULL,
    total DECIMAL(18,2) NOT NULL,
    shipping_fee DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    created_date DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    expired_date DATETIME2,
    expired BIT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    del_if BIT NOT NULL DEFAULT 0,
    account_id BIGINT,
    store_id BIGINT,
    CONSTRAINT ck_orders_total CHECK (total >= 0),
    CONSTRAINT ck_orders_shipping_fee CHECK (shipping_fee >= 0),
    CONSTRAINT ck_orders_status CHECK (
        status IN (
            'PENDING',
            'PAID',
            'PROCESSING',
            'SHIPPED',
            'DELIVERED',
            'CANCELLED'
        )
    ),
    CONSTRAINT fk_orders_accounts FOREIGN KEY (account_id) REFERENCES accounts(id),
    CONSTRAINT fk_orders_stores FOREIGN KEY (store_id) REFERENCES stores(id)
);

CREATE TABLE orders_details (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    product_name NVARCHAR(100) NOT NULL,
    product_price DECIMAL(18,2) NOT NULL,
    quantity INT NOT NULL,
    subtotal DECIMAL(18,2) NOT NULL,
    order_id BIGINT NOT NULL,
    product_id BIGINT NULL,
    CONSTRAINT ck_order_details_product_price CHECK (product_price >= 0),
    CONSTRAINT ck_order_details_quantity CHECK (quantity > 0),
    CONSTRAINT ck_order_details_subtotal CHECK (subtotal >= 0),
    CONSTRAINT fk_order_details_orders FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_order_details_products FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE payments (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    paid BIT NOT NULL DEFAULT 0,
    amount DECIMAL(18,2) NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    payment_date DATETIME2 NULL,
    order_id BIGINT NOT NULL UNIQUE,
    CONSTRAINT ck_payments_amount CHECK (amount >= 0),
    CONSTRAINT ck_payments_payment_method CHECK (
        payment_method IN ('COD', 'E_BANKING')
    ),
    CONSTRAINT fk_payments_orders FOREIGN KEY (order_id) REFERENCES orders(id)
);

CREATE TABLE carts (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    created_date DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    expired_date DATETIME2,
    expired BIT NOT NULL DEFAULT 0,
    total DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    del_if BIT NOT NULL DEFAULT 0,
    account_id BIGINT,
    CONSTRAINT ck_carts_total CHECK (total >= 0),
    CONSTRAINT fk_carts_accounts FOREIGN KEY (account_id) REFERENCES accounts(id)
);

CREATE TABLE items (
    id INT IDENTITY(1,1) PRIMARY KEY,
    quantity INT NOT NULL,
    subtotal DECIMAL(18,2) NOT NULL,
    product_id BIGINT NOT NULL,
    cart_id BIGINT NOT NULL,
    CONSTRAINT ck_items_quantity CHECK (quantity > 0),
    CONSTRAINT ck_items_subtotal CHECK (subtotal >= 0),
    CONSTRAINT uq_items_cart_product UNIQUE (cart_id, product_id),
    CONSTRAINT fk_items_carts FOREIGN KEY (cart_id) REFERENCES carts(id),
    CONSTRAINT fk_items_products FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE discounts (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    percentage DECIMAL(5,2) NOT NULL,
    description NVARCHAR(255),
    created_date DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    expired_date DATETIME2,
    expired BIT NOT NULL DEFAULT 0,
    del_if BIT NOT NULL DEFAULT 0,
    CONSTRAINT ck_discounts_percentage CHECK (percentage BETWEEN 0 AND 100)
);

CREATE TABLE products_discounts (
    product_id BIGINT NOT NULL,
    discount_id BIGINT NOT NULL,
    CONSTRAINT pk_products_discounts PRIMARY KEY (product_id, discount_id),
    CONSTRAINT fk_products_discounts_products FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_products_discounts_discounts FOREIGN KEY (discount_id) REFERENCES discounts(id)
);

CREATE TABLE comments (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    content NVARCHAR(500) NOT NULL,
    created_date DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    del_if BIT NOT NULL DEFAULT 0,
    product_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    CONSTRAINT fk_comments_products FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_comments_accounts FOREIGN KEY (account_id) REFERENCES accounts(id)
);

CREATE TABLE replies (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    content NVARCHAR(500) NOT NULL,
    created_date DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    del_if BIT NOT NULL DEFAULT 0,
    comment_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    CONSTRAINT fk_replies_comments FOREIGN KEY (comment_id) REFERENCES comments(id),
    CONSTRAINT fk_replies_accounts FOREIGN KEY (account_id) REFERENCES accounts(id)
);

CREATE TABLE chat_messages (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    content NVARCHAR(500) NOT NULL,
    created_date DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    sender_id BIGINT NULL,
    receiver_id BIGINT NULL,
    CONSTRAINT ck_chat_messages_participants CHECK (
        sender_id IS NOT NULL OR receiver_id IS NOT NULL
    ),
    CONSTRAINT fk_chat_messages_sender FOREIGN KEY (sender_id) REFERENCES accounts(id),
    CONSTRAINT fk_chat_messages_receiver FOREIGN KEY (receiver_id) REFERENCES accounts(id)
);

CREATE INDEX ix_products_category_id ON products(category_id);
CREATE INDEX ix_product_images_product_id ON product_images(product_id);
CREATE INDEX ix_orders_account_id ON orders(account_id);
CREATE INDEX ix_orders_store_id ON orders(store_id);
CREATE INDEX ix_order_details_order_id ON orders_details(order_id);
CREATE INDEX ix_order_details_product_id ON orders_details(product_id);
CREATE INDEX ix_carts_account_id ON carts(account_id);
CREATE INDEX ix_items_product_id ON items(product_id);
CREATE INDEX ix_products_discounts_discount_id ON products_discounts(discount_id);
CREATE INDEX ix_comments_product_id ON comments(product_id);
CREATE INDEX ix_comments_account_id ON comments(account_id);
CREATE INDEX ix_replies_comment_id ON replies(comment_id);
CREATE INDEX ix_replies_account_id ON replies(account_id);
CREATE INDEX ix_chat_messages_sender_id ON chat_messages(sender_id);
CREATE INDEX ix_chat_messages_receiver_id ON chat_messages(receiver_id);

GO

INSERT INTO roles (code, name)
VALUES
    ('ADMIN', N'Administrator'),
    ('USER', N'User');

INSERT INTO accounts (
    username,
    password,
    fullname,
    email,
    created_date,
    credential_expired_date
)
VALUES
(
    'admin',
    '$2a$10$OQV2lk31K/eTmbHEP0ljiue92qx/2WG.wWjwfDeyazOtNCapbOYPq',
    N'Administrator',
    'admin@example.com',
    SYSDATETIME(),
    DATEADD(YEAR, 10, SYSDATETIME())
),
(
    'georgefloyd',
    '$2a$10$OQV2lk31K/eTmbHEP0ljiue92qx/2WG.wWjwfDeyazOtNCapbOYPq',
    N'George Floyd',
    'georgefloyd@example.com',
    SYSDATETIME(),
    DATEADD(YEAR, 10, SYSDATETIME())
);

INSERT INTO account_role (account_id, role_id)
SELECT a.id, r.id
FROM accounts a
CROSS JOIN roles r
WHERE a.username = 'admin' AND r.code = 'ADMIN';

INSERT INTO account_role (account_id, role_id)
SELECT a.id, r.id
FROM accounts a
CROSS JOIN roles r
WHERE a.username IN ('admin', 'georgefloyd') AND r.code = 'USER';

INSERT INTO categories (name, description)
VALUES
    (N'Flower Basket', N'Flower arrangements presented in a basket'),
    (N'Bouquet', N'Hand-tied flower bouquet arrangements'),
    (N'Table Plant', N'Compact potted greenery and decorative plants for desks and tables'),
    (N'Flower Stand', N'Flower arrangements displayed on a stand');
GO
