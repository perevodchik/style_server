CREATE TABLE conversions (id SERIAL, client_id  INT NOT NULL REFERENCES users (id) ON DELETE CASCADE, master_id  INT NOT NULL REFERENCES users (id) ON DELETE CASCADE, last_message_id INT NULL REFERENCES messages (id) ON DELETE SET NULL, PRIMARY KEY (id));

CREATE TABLE messages (
    id SERIAL,
    sender_id INT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    conversion_id INT NOT NULL REFERENCES conversions (id) ON DELETE CASCADE,
    message TEXT NOT NULL DEFAULT '',
    has_media BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE conversion_users (
    user_id INT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    conversion_id INT NOT NULL REFERENCES conversions (id) ON DELETE CASCADE,
    last_read_message_id BOOLEAN NOT NULL DEFAULT FALSE
);


SELECT m.id, m.sender_id, m.message, m.has_media, m.created_at, u.name, u.surname, u.avatar
FROM messages m RIGHT JOIN users u ON u.id = m.sender_id WHERE m.conversion_id = 1 ORDER BY m.id DESC OFFSET 0 LIMIT 100;

SELECT c.id, m.id as message_id, m.sender_id as sender, m.message, m.has_media, m.created_at, u.name, u.surname, u.avatar
FROM conversions c
RIGHT JOIN messages m ON m.id = c.last_message_id
RIGHT JOIN users u ON u.id = c.master_id
WHERE c.client_id = 2 ORDER BY message_id DESC;