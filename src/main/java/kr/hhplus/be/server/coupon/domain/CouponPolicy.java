package kr.hhplus.be.server.coupon.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "coupon_policy")
@Getter @Setter
public class CouponPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "policy_id")
    private Long policyId;

    @Column(name = "code")
    private String code;         // 쿠폰 코드 (ex: WELCOME10)

    @Column(name = "discount_rate")
    private int discountRate;    // 할인율 (%)

    @Column(name = "max_count")
    private int maxCount;        // 발급 가능한 최대 수량

}
