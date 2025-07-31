package kr.hhplus.be.server.presentation.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CommonResultCode {
    // point
    GETPOINT_SUCCESS(200, "PO01", "잔액 조회 성공"),
    CHARGEPOINT_SUCCESS(200, "PO02", "잔액 충전에 성공하였습니다. ");

    private final int status;
    private final String code;
    private final String message;
}
