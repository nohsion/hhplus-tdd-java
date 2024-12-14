package io.hhplus.tdd.point;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/point")
public class PointController {

    private static final Logger log = LoggerFactory.getLogger(PointController.class);

    private final PointService pointService;

    public PointController(PointService pointService) {
        this.pointService = pointService;
    }

    @GetMapping("{id}")
    public UserPoint point(
            @PathVariable("id") long id
    ) {
        return pointService.getPointByUserId(id);
    }

    @GetMapping("{id}/histories")
    public List<PointHistory> history(
            @PathVariable("id") long id
    ) {
        return pointService.getPointHistoryByUserId(id);
    }

    @PatchMapping("{id}/charge")
    public UserPoint charge(
            @PathVariable("id") long id,
            @RequestBody long amount
    ) {
        return pointService.charge(id, amount);
    }

    /**
     * TODO - 특정 유저의 포인트를 사용하는 기능을 작성해주세요.
     */
    @PatchMapping("{id}/use")
    public UserPoint use(
            @PathVariable("id") long id,
            @RequestBody long amount
    ) {
        return new UserPoint(0, 0, 0);
    }
}
