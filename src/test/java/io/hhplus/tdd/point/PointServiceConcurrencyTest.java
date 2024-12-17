package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
class PointServiceConcurrencyTest {

    private static final Logger log = LoggerFactory.getLogger(PointServiceConcurrencyTest.class);
    private static final int THREAD_COUNT = 10;
    private static final long USER_ID_1L = 1L;
    private static final long USER_ID_2L = 2L;

    @Autowired
    private PointService pointService;

    @Autowired
    private UserPointTable userPointTable;

    @Autowired
    private PointHistoryTable pointHistoryTable;

    @BeforeEach
    void setUp() {
        userPointTable.insertOrUpdate(USER_ID_1L, 0L);
        userPointTable.insertOrUpdate(USER_ID_2L, 0L);
    }

    @DisplayName("동시에 10명이 포인트 충전을 요청해도 모두 순서대로 처리된다.")
    @Test
    void chargeConcurrently() throws InterruptedException {
        long userId = USER_ID_1L;
        long amount = 100;

        ExecutorService executorService = Executors.newFixedThreadPool(30);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            int finalI = i;
            executorService.submit(() -> {
                try {
                    log.info("{}번째 충전 요청: {}", finalI, amount);
                    pointService.charge(userId, amount);
                } finally {
                    log.info("{}번째 충전 완료: {}", finalI, amount);
                    latch.countDown();
                }
            });
        }
        latch.await();
        executorService.shutdown();

        UserPoint userPoint = userPointTable.selectById(userId);
        assertThat(userPoint.point())
                .as("동시에 10명이 100원씩 충전했으므로 총 1000원이어야 한다.")
                .isEqualTo(1000);

        List<PointHistory> pointHistoryList = pointHistoryTable.selectAllByUserId(userId);
        assertThat(pointHistoryList).hasSize(THREAD_COUNT);

        List<PointHistory> sortedHistory = pointHistoryList.stream()
                .sorted(Comparator.comparingLong(PointHistory::updateMillis))
                .toList();
        assertThat(sortedHistory)
                .as("충전 내역은 순서대로 처리되어야 한다.") // 이게 맞나?
                .isEqualTo(pointHistoryList);
    }

    @DisplayName("동시에 10명이 포인트 사용을 요청해도 모두 순서대로 처리된다.")
    @Test
    void useConcurrently() throws InterruptedException {
        long userId = USER_ID_2L;
        long amount = 100;

        ExecutorService executorService = Executors.newFixedThreadPool(30);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        pointService.charge(userId, 1000); // 1000원 충전시켜 놓음.

        for (int i = 0; i < THREAD_COUNT; i++) {
            int finalI = i;
            executorService.submit(() -> {
                try {
                    log.info("{}번째 사용 요청: {}", finalI, amount);
                    pointService.use(userId, amount);
                } finally {
                    log.info("{}번째 사용 완료: {}", finalI, amount);
                    latch.countDown();
                }
            });
        }
        latch.await();
        executorService.shutdown();

        UserPoint userPoint = userPointTable.selectById(userId);
        assertThat(userPoint.point())
                .as("동시에 10명이 100원씩 사용했으므로 총 1000원이 차감되어야 한다.")
                .isZero();

        List<PointHistory> pointHistoryList = pointHistoryTable.selectAllByUserId(userId);
        assertThat(pointHistoryList).hasSize(THREAD_COUNT + 1); // 충전 내역 1 + 사용 내역 10

        List<PointHistory> sortedHistory = pointHistoryList.stream()
                .sorted(Comparator.comparingLong(PointHistory::updateMillis))
                .toList();
        assertThat(sortedHistory)
                .as("사용 내역은 순서대로 처리되어야 한다.") // 이게 맞나?
                .isEqualTo(pointHistoryList);
    }
}
