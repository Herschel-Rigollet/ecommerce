package kr.hhplus.be.server.user.presentation.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChargeRequest {
    private long amount;

    public ChargeRequest(long amount) {
        this.amount = amount;
    }
}
