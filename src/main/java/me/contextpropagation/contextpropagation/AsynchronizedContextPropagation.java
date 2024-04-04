package me.contextpropagation.contextpropagation;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadLocalRandom;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AsynchronizedContextPropagation {

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
        ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();
        // 쓰레드 간에 인터리빙 시, 기존 쓰레드 로컬 사용하던 방식으로는 ContextPropagation을 할 수 없음.
        CompletableFuture<Void> future = CompletableFuture
                .runAsync(() -> initRequest()) // Worker-1번에 의해서 생성됨. -> 이벤트 ID 초기화 됨.
                .thenRunAsync(() -> addProduct("test-product"), forkJoinPool) // Worker-2번이 실행함. 그런데 1번에 의해 이벤트 ID가 전달되지 않았음.
                .thenRunAsync(() -> notifyShop("test-product"), forkJoinPool); // Worker-2~3번이 실행함. 당연히 이벤트 ID가 전달되지 않았음.

        future.join();
    }

    public static void main(String[] args) {
        handleRequest();
    }

}
