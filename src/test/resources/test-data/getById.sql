INSERT INTO publisher (id, name, is_deleted) VALUES (1, '技術出版社', FALSE);
INSERT INTO publisher (id, name, is_deleted) VALUES (2, '科学出版社', TRUE);

INSERT INTO `user` (id, name, is_deleted) VALUES (100, 'テストユーザー', FALSE);
INSERT INTO `user` (id, name, is_deleted) VALUES (101, '佐藤花子', TRUE);

INSERT INTO books (id, title, title_kana, author, publisher_id, user_id, price, is_deleted, created_at, updated_at)
VALUES
    (1, 'Kotlin入門', 'コトリン ニュウモン', '山田太郎', 1, 100, 2500, FALSE, '2023-01-01 10:00:00', '2023-01-01 10:00:00'),
    (2, 'PHP入門', 'ピーエイチピー ニュウモン', '田中太郎', 1, 100, 2000, TRUE, '2023-01-01 10:00:00', '2023-01-01 10:00:00'),
    (3, 'Java入門', 'ジャバー ニュウモン', '田中太郎', 2, 100, 2000, FALSE, '2023-01-01 10:00:00', '2023-01-01 10:00:00'),
    (4, 'Spring Boot 入門', 'スプリング ブート ニュウモン', '佐藤次郎', 1, 101, 3000, FALSE, '2023-02-01 10:00:00', '2023-02-01 10:00:00');