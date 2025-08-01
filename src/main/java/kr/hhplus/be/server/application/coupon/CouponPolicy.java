package kr.hhplus.be.server.application.coupon;

public class CouponPolicy {

    private Long id;

    private String code;         // 쿠폰 코드 (ex: WELCOME10)
    private int discountRate;    // 할인율 (%)
    private int maxCount;        // 발급 가능한 최대 수량

    public CouponPolicy(String code, int discountRate, int maxCount) {
        this.code = code;
        this.discountRate = discountRate;
        this.maxCount = maxCount;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public int getDiscountRate() {
        return discountRate;
    }

    public int getMaxCount() {
        return maxCount;
    }
}
