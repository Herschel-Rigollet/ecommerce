package kr.hhplus.be.server.presentation.user;

import kr.hhplus.be.server.application.user.UserService;
import kr.hhplus.be.server.presentation.user.response.UserResponse;
import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.presentation.common.CommonResponse;
import kr.hhplus.be.server.presentation.common.CommonResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/points")
public class UserPointController {

    private final UserService userService;

    @PostMapping("/charge")
    public ResponseEntity<CommonResponse> charge(@PathVariable Long userId, @RequestBody long point) {
        userService.charge(userId, point);
        return ResponseEntity.ok(CommonResponse.of(CommonResultCode.CHARGE_POINT_SUCCESS));
    }

    @PostMapping("/use/{userId}")
    public ResponseEntity<CommonResponse> usePoint(@PathVariable Long userId, @RequestBody long point) {
        userService.usePoint(userId, point);
        return ResponseEntity.ok(CommonResponse.of(CommonResultCode.USE_POINT_SUCCESS));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<CommonResponse> getPoint(@PathVariable Long userId) {
        User user = userService.getPointByUserId(userId);
        return ResponseEntity.ok(CommonResponse.of(CommonResultCode.GET_POINT_SUCCESS, UserResponse.from(user)));
    }
}
