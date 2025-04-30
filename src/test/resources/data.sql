INSERT INTO publisher (id, name, is_deleted) VALUES (1, '技術出版社', FALSE);
INSERT INTO publisher (id, name, is_deleted) VALUES (2, '教育出版社', FALSE);

INSERT INTO `user` (id, name, is_deleted) VALUES (100, 'テストユーザー', FALSE);
INSERT INTO `user` (id, name, is_deleted) VALUES (101, '佐藤花子', FALSE);

INSERT INTO books (title, title_kana, author, publisher_id, user_id, is_deleted, created_at, updated_at)
VALUES ('Kotlin入門', 'コトリン ニュウモン', '山田太郎', 1, 100, FALSE, '2023-01-01 10:00:00', '2023-01-01 10:00:00');