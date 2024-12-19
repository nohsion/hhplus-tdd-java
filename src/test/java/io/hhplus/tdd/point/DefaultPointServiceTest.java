package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultPointServiceTest {

    private PointService sut;

    private UserPointTable userPointTable;

    private PointHistoryTable pointHistoryTable;

    @BeforeEach
    void setUp() {
        userPointTable = mock(UserPointTable.class);
        pointHistoryTable = mock(PointHistoryTable.class);
        sut = new DefaultPointService(userPointTable, pointHistoryTable);
    }

    @Test
    void getPointByUserId() {
        long userId = 1L;
        UserPoint userPoint = new UserPoint(userId, 1000, 0);

        when(userPointTable.selectById(anyLong()))
                .thenReturn(userPoint);

        UserPoint result = sut.getPointByUserId(userId);

        assertThat(result).isEqualTo(userPoint);
    }

    @Test
    void getPointHistoryByUserId() {
        long userId = 1L;
        List<PointHistory> pointHistoryList = List.of(
                new PointHistory(1, userId, 1000, TransactionType.CHARGE, 0),
                new PointHistory(2, userId, 1000, TransactionType.USE, 0)
        );

        when(pointHistoryTable.selectAllByUserId(anyLong()))
                .thenReturn(pointHistoryList);

        List<PointHistory> result = sut.getPointHistoryByUserId(userId);

        assertThat(result).isEqualTo(pointHistoryList);
    }

}