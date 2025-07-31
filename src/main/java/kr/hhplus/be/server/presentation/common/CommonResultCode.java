package kr.hhplus.be.server.presentation.common;

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

    // order
    ORDER_SUCCESS(200, "OR01", "주문 성공");

    private final int status;
    private final String code;
    private final String message;
}
