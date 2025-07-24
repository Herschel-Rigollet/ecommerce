package kr.hhplus.be.server.mapper;

import kr.hhplus.be.server.domain.User;
import kr.hhplus.be.server.dto.BalanceResponse;

public class BalanceMapper {
    public static BalanceResponse toDto(User balance) {
        return new BalanceResponse(balance.getUserId(), balance.getAmount());
    }
}
