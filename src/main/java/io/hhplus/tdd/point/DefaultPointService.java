package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class DefaultPointService implements PointService {

    private static final Logger log = LoggerFactory.getLogger(DefaultPointService.class);

    private final Map<Long, Lock> lockByUserId = new ConcurrentHashMap<>();

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
        UserPoint savedUserPoint;
        log.info("charge Lock 요청... userId={}, amount={}", userId, amount);
        Lock lock = lockByUserId.computeIfAbsent(userId, k -> new ReentrantLock(true));
        lock.lock();
        log.info("charge Lock 획득! userId={}, amount={}", userId, amount);
        try {
            // 주의: 조회를 하는 부분까지 Lock을 걸어야 한다.
            // 충전에만 Lock을 걸면 +100을 두 번해도 결과가 +100이 되는 문제가 발생할 수 있다. 조회시점의 데이터가 동일하기 때문이다.
            UserPoint userPoint = userPointTable.selectById(userId);
            long amountToSave = userPoint.plusPoint(amount);

            savedUserPoint = userPointTable.insertOrUpdate(userId, amountToSave);
            pointHistoryTable.insert(userId, amount, TransactionType.CHARGE, System.currentTimeMillis());
        } finally {
            log.info("charge Lock 해제! userId={}, amount={}", userId, amount);
            lock.unlock();
        }

        return savedUserPoint;
    }

    /**
     * 포인트 사용(차감) 정책
     * 1. 잔액만큼만 사용할 수 있다.
     * 2. 0 이하의 금액은 사용할 수 없다.
     */
    @Override
    public UserPoint use(long userId, long amount) {
        UserPoint savedUserPoint;
        log.info("use Lock 요청... userId={}, amount={}", userId, amount);
        Lock lock = lockByUserId.computeIfAbsent(userId, k -> new ReentrantLock(true));
        lock.lock();
        log.info("use Lock 획득! userId={}, amount={}", userId, amount);
        try {
            UserPoint userPoint = userPointTable.selectById(userId);
            long amountToSave = userPoint.minusPoint(amount);

            savedUserPoint = userPointTable.insertOrUpdate(userId, amountToSave);
            pointHistoryTable.insert(userId, amount, TransactionType.USE, System.currentTimeMillis());
        } finally {
            log.info("use Lock 해제! userId={}, amount={}", userId, amount);
            lock.unlock();
        }

        return savedUserPoint;
    }
}
