package kr.hhplus.be.server.dto;

public class CouponIssueRequest {
    private int discountAmount;

    public int getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(int discountAmount) {
        this.discountAmount = discountAmount;
    }
}
