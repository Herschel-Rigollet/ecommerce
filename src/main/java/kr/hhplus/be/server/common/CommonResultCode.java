package kr.hhplus.be.server.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CommonResultCode {
    // point
    GET_POINT_SUCCESS(200, "PO01", "잔액 조회 성공"),
    CHARGE_POINT_SUCCESS(200, "PO02", "잔액 충전 성공"),
    USE_POINT_SUCCESS(200, "PO02", "잔액 사용 성공"),

    // product
    GET_PRODUCT_SUCCESS(200, "PR01", "상품 상세 조회 성공"),
    GET_POPULAR_PRODUCTS_SUCCESS(200, "PR02", "상위 상품 조회 성공"),

    // order
    ORDER_SUCCESS(200, "OR01", "주문 성공"),

    // coupon
    ISSUE_COUPON_SUCCESS(200, "CO01", "쿠폰 발급 성공"),
    GET_COUPON_SUCCESS(200, "CO02", "쿠폰 조회 성공"),
    COUPON_SOLD_OUT(400, "CO11", "쿠폰이 모두 소진되었습니다");

    private final int status;
    private final String code;
    private final String message;
}
