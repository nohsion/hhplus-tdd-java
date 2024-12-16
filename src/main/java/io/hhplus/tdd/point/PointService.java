package io.hhplus.tdd.point;

import java.util.List;

public interface PointService {
    UserPoint getPointByUserId(long userId);

    List<PointHistory> getPointHistoryByUserId(long userId);

    UserPoint charge(long userId, long amount);

    UserPoint use(long userId, long amount);
}
