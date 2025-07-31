package kr.hhplus.be.server.presentation.common;

import lombok.Getter;

public class CommonResponse {
    private int status;
    private String code;
    private String message;
    private Object data;

    public CommonResponse(CommonResultCode resultCode, Object data) {
        this.status = resultCode.getStatus();
        this.code = resultCode.getCode();
        this.message = resultCode.getMessage();
        this.data = data;
    }

    public static CommonResponse of(CommonResultCode resultCode, Object data) {
        return new CommonResponse(resultCode, data);
    }

    public static CommonResponse of(CommonResultCode resultCode) {
        return new CommonResponse(resultCode, "");
    }
}
