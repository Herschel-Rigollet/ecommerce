package kr.hhplus.be.server.user.presentation;

import kr.hhplus.be.server.user.application.UserService;
import kr.hhplus.be.server.user.presentation.dto.response.UserResponse;
import kr.hhplus.be.server.user.domain.User;
import kr.hhplus.be.server.common.CommonResponse;
import kr.hhplus.be.server.common.CommonResultCode;
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
