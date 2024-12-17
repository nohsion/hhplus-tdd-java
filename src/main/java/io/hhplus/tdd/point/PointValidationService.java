package io.hhplus.tdd.point;

import io.hhplus.tdd.database.UserPointTable;
import org.springframework.stereotype.Component;

@Component
public class PointValidationService {

    private static final long USER_MAX_POINT = 100_000_000L;
    private static final int ONCE_CHARGE_MAX_POINT = 10_000;

    private final UserPointTable userPointTable;

    public PointValidationService(UserPointTable userPointTable) {
        this.userPointTable = userPointTable;
    }

    public void validateCharge(long userId, long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("0 이하의 금액을 충전할 수 없습니다.");
        }
        if (amount > ONCE_CHARGE_MAX_POINT) {
            throw new IllegalArgumentException("한번에 충전 가능한 금액을 초과했습니다.");
        }
        UserPoint userPoint = userPointTable.selectById(userId);
        long amountToSave = userPoint.plusPoint(amount);
        if (amountToSave > USER_MAX_POINT) {
            throw new IllegalArgumentException("최대 잔고를 초과하여 충전할 수 없습니다.");
        }
    }

    public void validateUse(long userId, long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("0 이하의 금액을 사용할 수 없습니다.");
        }
        UserPoint userPoint = userPointTable.selectById(userId);
        long amountToSave = userPoint.minusPoint(amount);
        if (amountToSave < 0) {
            throw new IllegalArgumentException("잔액이 부족합니다.");
        }
    }
}
