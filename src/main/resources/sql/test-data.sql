-- 테스트 사용자 데이터 생성
INSERT INTO users (user_id, point, version) VALUES (1, 10000000, 0);
INSERT INTO users (user_id, point, version) VALUES (2, 10000000, 0);
INSERT INTO users (user_id, point, version) VALUES (3, 10000000, 0);
INSERT INTO users (user_id, point, version) VALUES (4, 1000, 0); -- 포인트 부족 사용자

-- 테스트 상품 데이터 생성
INSERT INTO product (product_id, product_name, price, stock) VALUES (1, '인기 상품 1', 10000, 100);
INSERT INTO product (product_id, product_name, price, stock) VALUES (2, '인기 상품 2', 20000, 100);
INSERT INTO product (product_id, product_name, price, stock) VALUES (3, '인기 상품 3', 30000, 100);
INSERT INTO product (product_id, product_name, price, stock) VALUES (4, '한정 상품', 50000, 10); -- 적은 재고

-- 테스트 쿠폰 정책 데이터 생성
INSERT INTO coupon_policy (policy_id, code, discount_rate, max_count) VALUES (1, 'WELCOME10', 10, 100);
INSERT INTO coupon_policy (policy_id, code, discount_rate, max_count) VALUES (2, 'SPECIAL20', 20, 50);
INSERT INTO coupon_policy (policy_id, code, discount_rate, max_count) VALUES (3, 'LIMITED', 30, 10); -- 한정 쿠폰

-- 비동기 통합테스트용 쿠폰 정책 데이터 추가
INSERT INTO coupon_policy (policy_id, code, discount_rate, max_count) VALUES (4, 'ASYNC_TEST_COUPON', 15, 100);
INSERT INTO coupon_policy (policy_id, code, discount_rate, max_count) VALUES (5, 'LIMITED_ASYNC_50', 20, 50);
INSERT INTO coupon_policy (policy_id, code, discount_rate, max_count) VALUES (6, 'ORDER_TEST_5', 25, 5);
INSERT INTO coupon_policy (policy_id, code, discount_rate, max_count) VALUES (7, 'PERF_TEST_100', 10, 100);