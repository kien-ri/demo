CREATE TABLE posts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE books (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255),
    title_kana VARCHAR(255),
    author VARCHAR(255),
    publisher_id BIGINT,
    user_id BIGINT,
    is_deleted BOOLEAN NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
INSERT INTO books (title, title_kana, author, publisher_id, user_id, is_deleted) VALUES
('ONE PIECE', 'ワンピース', '尾田栄一郎', 1, 1, FALSE),
('進撃の巨人', 'シンゲキノキョジン', '諫山創', 2, 2, FALSE),
('ソードアート・オンライン', 'ソードアート・オンライン', '川原礫', 3, 1, FALSE),
('名探偵コナン', 'メイタンテイコナン', '青山剛昌', 4, 3, FALSE),
('鬼滅の刃', 'キメツノヤイバ', '吾峠呼世晴', 1, 2, FALSE),
('転生したらスライムだった件', 'テンセイシタラスライムダッタケン', '伏瀬', 3, 4, FALSE),
('SPY×FAMILY', 'スパイファミリー', '遠藤達哉', 1, 5, FALSE),
('君の名は。', 'キミノナハ', '新海誠', 3, 3, FALSE),
('ハリー・ポッターと賢者の石', 'ハリーポッタートケンジャノイシ', 'J.K.ローリング', NULL, 1, FALSE),
('ある夜、雪の中で', 'アルヨルユキノナカデ', 'アガサ・クリスティ', 5, 2, FALSE),
('そして誰もいなくなった', 'ソシテダレモイナクナッタ', 'アガサ・クリスティ', 5, 3, TRUE);

CREATE TABLE publisher (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    name_kana VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
INSERT INTO publisher (name, name_kana, is_deleted) VALUES
('集英社', 'シュウエイシャ', FALSE),
('講談社', 'コウダンシャ', FALSE),
('KADOKAWA', 'カドカワ', FALSE),
('小学館', 'ショウガクカン', FALSE),
('幻冬舎', 'ゲントウシャ', FALSE);

CREATE TABLE user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    login_id VARCHAR(255),
    password VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
INSERT INTO user (name, login_id, password, is_deleted) VALUES
('山田太郎', 'yamada.taro', 'password123', FALSE),
('佐藤花子', 'satou.hanako', 'securepass', FALSE),
('田中一郎', 'tanaka.ichiro', 'mysecret', FALSE),
('鈴木美咲', 'suzuki.misaki', 'testuser', FALSE),
('高橋健太', 'takahashi.kenta', 'anotherpw', FALSE);