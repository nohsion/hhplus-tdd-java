package io.hhplus.tdd.point;

import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PointValidationServiceTest {

    private PointValidationService sut;

    private UserPointTable userPointTable;

    @BeforeEach
    void setUp() {
        userPointTable = mock(UserPointTable.class);
        sut = new PointValidationService(userPointTable);
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
        assertThatThrownBy(() -> sut.validateCharge(userId, amount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("최대 잔고를 초과하여 충전할 수 없습니다.");
    }

    @DisplayName("한번에 충전 가능한 금액이 일정 금액을 초과한다면 충전은 실패한다.")
    @Test
    void chargeFailWhenOnceMaxPoint() {
        long userId = 1L;
        long amount = 10_001L;

        assertThatThrownBy(() -> sut.validateCharge(userId, amount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("한번에 충전 가능한 금액을 초과했습니다.");
    }

    @DisplayName("0 이하의 금액을 충전할 수 없다.")
    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void chargeFailWhenMinusAmount(long amount) {
        long userId = 1L;

        assertThatThrownBy(() -> sut.validateCharge(userId, amount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("0 이하의 금액을 충전할 수 없습니다.");
    }

    @DisplayName("0 이하의 금액을 사용할 수 없다.")
    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void useFailWhenMinusAmount(long amount) {
        long userId = 1L;

        assertThatThrownBy(() -> sut.validateUse(userId, amount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("0 이하의 금액을 사용할 수 없습니다.");
    }

    @DisplayName("잔액보다 큰 금액을 사용할 수 없다.")
    @Test
    void useFailWhenOverAmount() {
        long userId = 1L;
        long exisitingAmount = 100L;
        long useAmount = 101L;
        UserPoint userPoint = new UserPoint(userId, exisitingAmount, 0);

        when(userPointTable.selectById(anyLong()))
                .thenReturn(userPoint);

        assertThatThrownBy(() -> sut.validateUse(userId, useAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("잔액이 부족합니다.");
    }

}