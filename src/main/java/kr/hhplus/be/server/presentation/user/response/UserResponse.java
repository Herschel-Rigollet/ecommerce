package kr.hhplus.be.server.presentation.user.response;

import kr.hhplus.be.server.domain.user.User;
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
                .userId(user.getId())
                .point(user.getPoint())
                .build();
    }
}
