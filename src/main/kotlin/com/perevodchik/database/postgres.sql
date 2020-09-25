-- CATEGORIES START
CREATE TABLE categories (
    id SERIAL,
    name TEXT NOT NULL,
    PRIMARY KEY (id)
)

CREATE TABLE sub_categories (
    id SERIAL,
    name TEXT NOT NULL,
    parent_category INT NOT NULL REFERENCES categories (id),
    PRIMARY KEY (id)
);

-- CATEGORIES END


-- CUSTOMERS START
CREATE TABLE customers (
    id SERIAL,
    phone TEXT NOT NULL,
    name TEXT,
    surname TEXT,
    registered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE users_images (
    id SERIAL,
    user_id INT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    image TEXT NOT NULL,
    PRIMARY KEY (id)
)

CREATE TABLE user_comments (
    id SERIAL,
    owner_id INT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    target_user_id INT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    comment_text TEXT NOT NULL,
    rate REAL NOT NULL DEFAULT (5),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

-- CUSTOMERS END


-- MASERS START
CREATE TABLE masters (
    id SERIAL,
    phone TEXT NOT NULL,
    name TEXT,
    surname TEXT,
    registered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE master_images (
    id SERIAL,
    user_id INT NOT NULL REFERENCES masters (id) ON DELETE CASCADE,
    image TEXT NOT NULL,
    PRIMARY KEY (id)
)

CREATE TABLE master_comments (
    id SERIAL,
    owner_id INT NOT NULL REFERENCES customers (id) ON DELETE CASCADE,
    target_user_id INT NOT NULL REFERENCES masters (id) ON DELETE CASCADE,
    comment_text TEXT NOT NULL,
    rate REAL NOT NULL DEFAULT (5),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

-- MASERS END


-- CONVERSIONS START
CREATE TABLE conversions (
    id SERIAL,
    client_id INT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    master_id INT NOT NULL REFERENCES USERS (id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE messages (
    id SERIAL,
    conversion_id INT NOT NULL REFERENCES USERS (id) ON DELETE CASCADE,
    sender_id INT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    message TEXT NOT NULL,
    has_media BOOL NOT NULL DEFAULT (0),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
)

-- CONVERSIONS END

-- SENTENCES START
CREATE TABLE sentences (
    id SERIAL,
    master_id INT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    message TEXT NOT NULL,
    price INT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (ic)
);

CREATE TABLE sentence_comments (
    id SERIAL,
    sentence_id INT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    sender_id INT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    message TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (ic)
);

-- SENTENCES END


-- ORDERS START
CREATE TABLE orders (
    id SERIAL,
    client_id INT NOT NULL,
    master_id INT NULL,
    price INT NOT NULL DEFAULT (0),
    sketch_id INT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE order_categories (
    id SERIAL,
    category_id INT NOT NULL REFERENCES sub_categories (id),
    order_id INT NOT NULL REFERENCES orders (id),
    PRIMARY KEY (id)
)

CREATE TABLE order_images (
    id SERIAL,
    order_id INT NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    image TEXT NOT NULL,
    PRIMARY KEY (id)
)

-- ORDERS END


-- SKETCHES START
CREATE TABLE sketches (
    id SERIAL,
    owner_id INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE styles (
    id SERIAL,
    ru_name TEXT,
    ua_name TEXT,
    en_name TEXT,
    PRIMARY LEY (id)
);

CREATE TABLE position (
    id SERIAL,
    ru_name TEXT,
    ua_name TEXT,
    en_name TEXT,
    PRIMARY LEY (id)
);

CREATE TABLE sketches_data (
    id SERIAL,
    style_id INT NOT NULL REFERENCES styles (id),
    position_id INT NOT NULL REFERENCES positions (id),
    PRIMARY KEY (id)
);

-- SKETCHES END

-- NOTIFICATIONS START
CREATE TABLE notifications (
    id SERIAL,
    status INT NOT NULL,
    order_id INT NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

-- NOTIFICATIONS END