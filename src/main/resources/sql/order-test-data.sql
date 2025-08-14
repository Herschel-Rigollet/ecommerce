-- 테스트 사용자 데이터 생성 (충분한 포인트)
INSERT INTO users (user_id, point, version) VALUES (1, 50000000, 0);
INSERT INTO users (user_id, point, version) VALUES (2, 50000000, 0);
INSERT INTO users (user_id, point, version) VALUES (3, 50000000, 0);
INSERT INTO users (user_id, point, version) VALUES (999, 1000, 0); -- 포인트 부족 사용자

-- 테스트 상품 데이터 생성
INSERT INTO product (product_id, product_name, price, stock) VALUES (1, '멀티락 테스트 상품 1', 10000, 100);
INSERT INTO product (product_id, product_name, price, stock) VALUES (2, '멀티락 테스트 상품 2', 20000, 100);
INSERT INTO product (product_id, product_name, price, stock) VALUES (3, '멀티락 테스트 상품 3', 30000, 100);
INSERT INTO product (product_id, product_name, price, stock) VALUES (4, '한정 상품', 50000, 50);  -- 적은 재고
INSERT INTO product (product_id, product_name, price, stock) VALUES (5, '초한정 상품', 100000, 10); -- 매우 적은 재고