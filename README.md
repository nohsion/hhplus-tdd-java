# 포인트 관리 서비스, TDD와 동시성 문제를 곁들인

# 요구사항 분석

## 포인트 충전 정책

1. 한번에 충전 가능한 금액이 정해져있다.
2. 사용자의 최대 잔고만큼만 저장할 수 있다.
3. 0 이하의 금액은 충전할 수 없다.

## 포인트 사용 정책

1. 잔액만큼만 사용할 수 있다.
2. 0 이하의 금액은 사용할 수 없다.

# 동시성 제어

## 독립성 보장

충전, 사용 요청이 동시에 들어와도 독립적으로 처리되어야 한다.

1. 요청마다 Lock을 걸어서 매 요청이 독립적으로 수행될 수 있도록 한다.
2. (도전과제) HashMap의 key인 userId 별로 독립적으로 동시성 처리를 진행한다.
   - userId=1의 충전(사용) 요청과 userId=2의 충전(사용) 요청은 동시에 처리되어도 무방하다.
   - 단, userId=1의 충전(사용) 요청이 동시에 들어온다면, 독립적으로 처리되어야 한다.
3. 조회 요청은 독립성을 보장하지 않아도 무방하다.
   - 자원을 read만 하기 때문에 동시성 제어를 하지 않아도 문제될 부분이 없다.

## 요청 순서 보장

독립적으로 처리되는게 보장되었다면, 요청 순서대로 처리해보자.

### 요청 순서 정의

- 기준
  1. 클라이언트 요청시각 
  2. 서버 요청시각 
  3. Lock 획득시각

#### 결정사항

3번인 **Lock 획득시각**을 기준으로 요청 순서를 정의한다.

Why?
- Lock 획득시각은 실제로 작업(충전, 사용)이 수행되는 시점과 일치한다. 또한, 서버 요청 -> Lock 획득 까지 시간이 매우 짧다. (중간에 Blocking 되는 부분이 없기도 하고, 서버 요청시각과 거의 일치하고 제일 편하다.)
- 클라이언트 요청시각: 서버에 나중에 들어온 요청이더라도 클라이언트 요청시각이 더 빠를 수 있다. 즉, 서버에 먼저 들어온 요청이더라도 바로 처리할 수 없고 대기해야 하는 불편함이 있다. 응답 시간이 지연되는 문제가 있다.
- 서버 요청시각: 클라이언트 요청과 마찬가지로 서버 요청시각을 별도로 기록하고 관리해야 한다.

# 동시성 제어 방식

## synchronized

- 한 스레드가 synchronized 블록에 들어가면, 실행이 끝날 때까지 나머지 스레드는 대기 (Blocking)
- 스레드가 synchronized 블록에 진입 시도
  - 사용 중이 아니면, Lock을 획득하고 블록 실행
  - 사용 중이면, Lock이 해제될 때까지 대기 상태
- 따라서, 동시 요청 스레드가 많을수록 성능이 매우 떨어진다.

## Lock

동시성 제어를 관리해주는 `java.util.concurrent.locks` 패키지의 `ReentrantLock` 사용하면 명시적으로 Lock을 관리할 수 있다.

```java
Lock lock = new ReentrantLock();
lock.lock();
try {
    // access to the shared resource
} finally {
    lock.unlock();
}
```

#### 공정성(fairness)

ReentrantLock 생성자에는 boolean 타입의 fairness 공정성 옵션을 제공한다.
디폴트는 false이며, true로 설정하면 순서를 유지할 수 있다.

- 예시: 스레드 A가 lock을 획득하고 있고, 스레드 B가 lock을 요청했다면, B는 대기 상태로 들어간다. A가 lock을 해제하는 순간에 스레드 C가 lock을 요청하면, C는 B가 깨어나기도 전에 lock을 얻을 수 있다.
- `fairness=true` 로 설정하면, 대기 순서에 따라 B가 먼저 lock을 얻을 수 있다.

## ConcurrentHashMap

`ConcurrentHashMap`은 내부적으로 Lock을 사용해서 동시성 문제를 해결한다.

### 결정사항

`ReentrantLock`을 채택하였다.

Why?
- 위 동시성 제어 방식들 모두 독립성을 보장하지만, 요청 순서를 보장하지 않는다.
- 순서를 보장하기 위해서는 `ReentrantLock`의 fairness 옵션을 사용하면 된다.


# 참고
- [ReentrantLock이란?.md](https://github.com/wjdrbs96/Today-I-Learn/blob/master/Java/Thread/java.util.concurrent.locks/ReentrantLock%EC%9D%B4%EB%9E%80%3F.md)
- [[Java] 동시성 문제](https://velog.io/@nohsion/Java-Concurrent-Problem)
