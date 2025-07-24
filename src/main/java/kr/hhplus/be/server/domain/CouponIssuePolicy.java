package kr.hhplus.be.server.domain;

public class CouponIssuePolicy {
    private final int maxIssueCount;
    private int issuedCount;

    public CouponIssuePolicy(int maxIssueCount) {
        this.maxIssueCount = maxIssueCount;
        this.issuedCount = 0;
    }

    public void increaseIssued() {
        if (issuedCount >= maxIssueCount) {
            throw new IllegalStateException("쿠폰 발급 수량을 초과했습니다.");
        }
        issuedCount++;
    }

    public int getIssuedCount() {
        return issuedCount;
    }

    public boolean isAvailable() {
        return issuedCount < maxIssueCount;
    }
}
