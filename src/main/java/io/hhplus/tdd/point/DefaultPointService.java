package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DefaultPointService implements PointService {

    private static final long USER_MAX_POINT = 100_000_000L;
    private static final int ONCE_CHARGE_MAX_POINT = 10_000;

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    public DefaultPointService(
            UserPointTable userPointTable,
            PointHistoryTable pointHistoryTable
    ) {
        this.userPointTable = userPointTable;
        this.pointHistoryTable = pointHistoryTable;
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
    public UserPoint charge(long userId, long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("0 이하의 금액을 충전할 수 없습니다.");
        }
        if (amount > ONCE_CHARGE_MAX_POINT) {
            throw new IllegalArgumentException("한번에 충전 가능한 금액을 초과했습니다.");
        }
        UserPoint userPoint = userPointTable.selectById(userId);
        long amountToSave = userPoint.point() + amount;
        if (amountToSave > USER_MAX_POINT) {
            throw new IllegalArgumentException("최대 잔고를 초과하여 충전할 수 없습니다.");
        }

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
    public UserPoint use(long userId, long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("0 이하의 금액을 사용할 수 없습니다.");
        }
        UserPoint userPoint = userPointTable.selectById(userId);
        long amountToSave = userPoint.point() - amount;
        if (amountToSave < 0) {
            throw new IllegalArgumentException("잔액이 부족합니다.");
        }

        UserPoint savedUserPoint = userPointTable.insertOrUpdate(userId, amountToSave);
        pointHistoryTable.insert(userId, amount, TransactionType.USE, System.currentTimeMillis());

        return savedUserPoint;
    }
}
