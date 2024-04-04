package me.contextpropagation.contextpropagation;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadLocalRandom;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MitigationAsynchronizedContextPropagationImprove {

    static class WrappedExecutor implements Executor {
        private final Executor actual;

        public WrappedExecutor(Executor actual) {
            this.actual = actual;
        }

        @Override
        public void execute(Runnable command) {
            this.actual.execute(new WrappedRunnable(command));
        }
    }

    static class WrappedRunnable implements Runnable {
        private final long correlationId;
        private final Runnable wrapped;

        public WrappedRunnable(Runnable wrapped) {
            // 이 객체를 생성해주는 쓰레드A의 이벤트 ID가 전달됨.
            // 이 객체는 다른 쓰레드B에서 동작함. 즉, 쓰레드 A -> 쓰레드 B로 Event ID가 Propagation 된 것임.
            this.correlationId = CORRELATION_ID.get();
            this.wrapped = wrapped;
            log("Wrapped Instance are created. ");
        }

        @Override
        public void run() {
            Long old = this.correlationId;
            CORRELATION_ID.set(old);
            try {
                wrapped.run();
            }
            finally {
                CORRELATION_ID.set(old);
            }
        }
    }

    static final ThreadLocal<Long> CORRELATION_ID = new ThreadLocal<>();

    static long correlationId() {
        return Math.abs(ThreadLocalRandom.current().nextLong());
    }

    static void log(String message) {
        String threadName = Thread.currentThread().getName();
        String threadNameTail = threadName.substring(
                Math.max(0, threadName.length() - 10));

        log.info("[{}][     {}] {}",
                 threadNameTail,
                 CORRELATION_ID.get(),
                 message);
    }

    static void initRequest() {
        CORRELATION_ID.set(correlationId());
        log.info("[{}][     {}] init.",
                 Thread.currentThread().getName(),
                 CORRELATION_ID.get());
    }

    static void addProduct(String productName) {
        log("Adding product:" + productName);
    }

    static void notifyShop(String productName) {
        log("Notifying shop about:" + productName);
    }

    static void handleRequest() {
        initRequest(); // 메인 쓰레드가 셋업.
        final WrappedExecutor executor = new WrappedExecutor(ForkJoinPool.commonPool());
        // 쓰레드 간에 인터리빙 시, 기존 쓰레드 로컬 사용하던 방식으로는 ContextPropagation을 할 수 없음.

        CompletableFuture<Void> future = CompletableFuture
                .runAsync(() -> addProduct("test-product"), executor) // Main 쓰레드가 생성함 -> Context 넘어감. -> Worker-1번이 실행함
                .thenRunAsync(() -> notifyShop("test-product"), executor); // Worker-1번이 실행함 -> Context 넘어감. -> Worker 2번이 실행함.

        future.join();
    }

    public static void main(String[] args) {
        handleRequest();
    }

}
