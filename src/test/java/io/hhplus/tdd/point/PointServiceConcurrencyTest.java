package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
class PointServiceConcurrencyTest {

    private static final int THREAD_COUNT = 10;
    private static final long USER_ID_1L = 1L;
    private static final long USER_ID_2L = 2L;

    private List<Long> amounts;

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
        amounts = LongStream.range(1L, THREAD_COUNT + 1L)
                .map(i -> i * 10L) // 10, 20, 30, ...
                .boxed()
                .toList();
    }

    @DisplayName("동시에 10명이 포인트 충전을 요청해도 모두 순서대로 처리되어야 한다.")
    @Test
    void chargeConcurrently() throws InterruptedException {
        long userId = USER_ID_1L;
        long totalAmount = amounts.stream().mapToLong(Long::longValue).sum();

        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            Thread.sleep(5); // 순서대로 처리되도록 간격을 둔다.
            long amount = amounts.get(i);
            executorService.submit(() -> {
                try {
                    pointService.charge(userId, amount);
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executorService.shutdown();

        // 동시성 테스트 (순서와 상관없이 처리)
        UserPoint userPoint = userPointTable.selectById(userId);
        assertThat(userPoint.point())
                .as("동시에 10명이 충전해도 결과 총 금액은 모두 더해져야 한다.")
                .isEqualTo(totalAmount);

        // 동시성 테스트 (순서대로 처리)
        List<PointHistory> pointHistoryList = pointHistoryTable.selectAllByUserId(userId);
        assertThat(pointHistoryList).hasSize(THREAD_COUNT);
        List<Long> historyAmounts = pointHistoryList.stream()
                .map(PointHistory::amount)
                .toList();
        assertThat(historyAmounts)
                .as("사용 내역은 순서대로 처리되어야 한다.")
                .containsExactlyElementsOf(amounts);
    }

    @DisplayName("동시에 10명이 포인트 사용을 요청해도 모두 순서대로 처리된다.")
    @Test
    void useConcurrently() throws InterruptedException {
        long userId = USER_ID_2L;
        long totalAmount = amounts.stream().mapToLong(Long::longValue).sum();

        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        pointService.charge(userId, totalAmount); // 충전

        for (int i = 0; i < THREAD_COUNT; i++) {
            Thread.sleep(5); // 순서대로 처리되도록 간격을 둔다.
            long amount = amounts.get(i);
            executorService.submit(() -> {
                try {
                    pointService.use(userId, amount);
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executorService.shutdown();

        // 동시성 테스트 (순서와 상관없이 처리)
        UserPoint userPoint = userPointTable.selectById(userId);
        assertThat(userPoint.point())
                .as("동시에 10명이 사용해도 결과 총 금액은 모두 차감되어야 한다.")
                .isZero();

        // 동시성 테스트 (순서대로 처리)
        List<PointHistory> pointHistoryList = pointHistoryTable.selectAllByUserId(userId);
        assertThat(pointHistoryList).hasSize(THREAD_COUNT + 1); // 충전 내역 1 + 사용 내역 20(=THREAD_COUNT)
        List<Long> historyAmounts = pointHistoryList.stream()
                .skip(1) // 충전 내역은 제외
                .map(PointHistory::amount)
                .toList();
        assertThat(historyAmounts)
                .as("사용 내역은 순서대로 처리되어야 한다.")
                .containsExactlyElementsOf(amounts);
    }
}
