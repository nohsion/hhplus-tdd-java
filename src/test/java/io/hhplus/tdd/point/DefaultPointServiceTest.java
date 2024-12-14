package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @DisplayName("사용자의 최대 잔고를 초과하여 충전할 수 없다.")
    @Test
    void chargeFailWhenUserMaxPoint() {
        // given
        long userId = 1;
        long amount = 100L;
        UserPoint userPoint = new UserPoint(userId, 99_999_999L, 0);

        // when
        when(userPointTable.selectById(anyLong()))
                .thenReturn(userPoint);

        // then
        assertThatThrownBy(() -> sut.charge(userId, amount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("최대 잔고를 초과하여 충전할 수 없습니다.");
    }

    @DisplayName("한번에 충전 가능한 금액이 일정 금액을 초과한다면 충전은 실패한다.")
    @Test
    void chargeFailWhenOnceMaxPoint() {
        long userId = 1L;
        long amount = 10_001L;

        assertThatThrownBy(() -> sut.charge(userId, amount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("한번에 충전 가능한 금액을 초과했습니다.");
    }

}