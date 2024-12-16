package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DefaultPointService implements PointService {

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;
    private final PointValidationService pointValidationService;

    public DefaultPointService(
            UserPointTable userPointTable,
            PointHistoryTable pointHistoryTable,
            PointValidationService pointValidationService
    ) {
        this.userPointTable = userPointTable;
        this.pointHistoryTable = pointHistoryTable;
        this.pointValidationService = pointValidationService;
    }

    @Override
    public UserPoint getPointByUserId(long userId) {
        return userPointTable.selectById(userId);
    }

    @Override
    public List<PointHistory> getPointHistoryByUserId(long userId) {
        return pointHistoryTable.selectAllByUserId(userId);
    }

    /**
     * 포인트 충전 정책
     * 1. 한번에 충전 가능한 금액이 정해져있다.
     * 2. 사용자의 최대 잔고만큼만 저장할 수 있다.
     * 3. 0 이하의 금액은 충전할 수 없다.
     */
    public synchronized UserPoint charge(long userId, long amount) {
        pointValidationService.validateCharge(userId, amount);

        UserPoint userPoint = userPointTable.selectById(userId);
        long amountToSave = userPoint.point() + amount;

        UserPoint savedUserPoint = userPointTable.insertOrUpdate(userId, amountToSave);
        pointHistoryTable.insert(userId, amount, TransactionType.CHARGE, System.currentTimeMillis());

        return savedUserPoint;
    }

    /**
     * 포인트 사용(차감) 정책
     * 1. 잔액만큼만 사용할 수 있다.
     * 2. 0 이하의 금액은 사용할 수 없다.
     */
    @Override
    public synchronized UserPoint use(long userId, long amount) {
        pointValidationService.validateUse(userId, amount);

        UserPoint userPoint = userPointTable.selectById(userId);
        long amountToSave = userPoint.point() - amount;

        UserPoint savedUserPoint = userPointTable.insertOrUpdate(userId, amountToSave);
        pointHistoryTable.insert(userId, amount, TransactionType.USE, System.currentTimeMillis());

        return savedUserPoint;
    }
}
