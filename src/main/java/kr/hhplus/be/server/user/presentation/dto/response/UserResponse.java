package kr.hhplus.be.server.user.presentation.dto.response;

import kr.hhplus.be.server.user.domain.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponse{
    private Long userId;
    private long point;

    public static UserResponse from(User user) {
        return UserResponse
                .builder()
                .userId(user.getUserId())
                .point(user.getPoint())
                .build();
    }
}
