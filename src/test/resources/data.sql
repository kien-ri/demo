INSERT INTO publisher (id, name, is_deleted) VALUES (1, '技術出版社', FALSE);
INSERT INTO publisher (id, name, is_deleted) VALUES (2, '教育出版社', FALSE);
INSERT INTO publisher (id, name, is_deleted) VALUES (3, '文芸出版社', FALSE);
INSERT INTO publisher (id, name, is_deleted) VALUES (4, '科学出版社', TRUE);
INSERT INTO publisher (id, name, is_deleted) VALUES (5, '歴史出版社', FALSE);

INSERT INTO `user` (id, name, is_deleted) VALUES (100, 'テストユーザー', FALSE);
INSERT INTO `user` (id, name, is_deleted) VALUES (101, '佐藤花子', FALSE);
INSERT INTO `user` (id, name, is_deleted) VALUES (102, '鈴木一郎', FALSE);
INSERT INTO `user` (id, name, is_deleted) VALUES (103, '高橋美穂', TRUE);
INSERT INTO `user` (id, name, is_deleted) VALUES (104, '中村健太', FALSE);

INSERT INTO books (title, title_kana, author, publisher_id, user_id, is_deleted, created_at, updated_at)
VALUES
    ('Kotlin入門', 'コトリン ニュウモン', '山田太郎', 1, 100, 2500, FALSE, '2023-01-01 10:00:00', '2023-01-01 10:00:00'),
    ('Java入門', 'ジャバー ニュウモン', '田中太郎', 2, 101, 2000, FALSE, '2023-01-01 10:00:00', '2023-01-01 10:00:00'),
    ('PHP入門', 'ピーエイチピー ニュウモン', '田中太郎', 2, 101, 2000, TRUE, '2023-01-01 10:00:00', '2023-01-01 10:00:00'),
    ('Spring Boot 入門', 'スプリング ブート ニュウモン', '佐藤次郎', 3, 102, 3000, FALSE, '2023-02-01 10:00:00', '2023-02-01 10:00:00'),
    ('データベース基礎', 'データベース キソ', '山本花子', 4, 102, 2500, FALSE, '2023-02-02 10:00:00', '2023-02-02 10:00:00'),
    ('アルゴリズム入門', 'アルゴリズム ニュウモン', '田中一', 3, 103, 3500, FALSE, '2023-02-03 10:00:00', '2023-02-03 10:00:00'),
    ('ネットワーク入門', 'ネットワーク ニュウモン', '高橋健', 4, 103, 2800, FALSE, '2023-02-04 10:00:00', '2023-02-04 10:00:00'),
    ('AI入門', 'エーアイ ニュウモン', '山田健太', 5, 104, 4000, TRUE, '2023-02-07 10:00:00', '2023-02-07 10:00:00');