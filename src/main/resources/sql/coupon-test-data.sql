-- 테스트 쿠폰 정책 데이터 생성
INSERT INTO coupon_policy (policy_id, code, discount_rate, max_count) VALUES (1, 'WELCOME100', 10, 100);
INSERT INTO coupon_policy (policy_id, code, discount_rate, max_count) VALUES (2, 'SPECIAL50', 20, 50);
INSERT INTO coupon_policy (policy_id, code, discount_rate, max_count) VALUES (3, 'LIMITED10', 30, 10);
INSERT INTO coupon_policy (policy_id, code, discount_rate, max_count) VALUES (4, 'FLASH5', 50, 5);   -- 초한정
INSERT INTO coupon_policy (policy_id, code, discount_rate, max_count) VALUES (5, 'MEGA1000', 15, 1000); -- 대량