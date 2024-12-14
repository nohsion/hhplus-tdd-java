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
     */
    public UserPoint charge(long userId, long amount) {
        if (amount > ONCE_CHARGE_MAX_POINT) {
            throw new IllegalArgumentException("한번에 충전 가능한 금액을 초과했습니다.");
        }
        UserPoint userPoint = userPointTable.selectById(userId);
        long amountToCharge = userPoint.point() + amount;
        if (amountToCharge > USER_MAX_POINT) {
            throw new IllegalArgumentException("최대 잔고를 초과하여 충전할 수 없습니다.");
        }

        UserPoint savedUserPoint = userPointTable.insertOrUpdate(userId, amountToCharge);
        pointHistoryTable.insert(userId, amount, TransactionType.CHARGE, System.currentTimeMillis());

        return savedUserPoint;
    }
}
