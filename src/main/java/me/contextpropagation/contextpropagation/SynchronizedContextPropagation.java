package me.contextpropagation.contextpropagation;

import java.util.concurrent.ThreadLocalRandom;

import lombok.extern.slf4j.Slf4j;

// 시작할 때, 쓰레드마다 가진 랜덤값 생성기를 통해서 랜덤한 값을 생성함.
// 이걸 특정 쓰레드가 하는 작업으로 생각할 수 있음.

// 특정 요청의 실행이 동일한 스레드에서 이루어지고, 다른 문제와 인터리빙 되지 않는 이상
// ThreadLocal을 사용하면 로깅에 사용되는 메타데이터에서 비즈니스 로직을 분리할 수 있음.
// 여기서는 이벤트 ID같은 느낌임.

@Slf4j
public class SynchronizedContextPropagation {
    static final ThreadLocal<Long> CORRELATION_ID = new ThreadLocal<>();

    static long correlationId() {
        // 이 각 스레드에서 자체적인 랜덤 수 생성기를 관리하기 때문에,
        // 여러 스레드가 동일한 랜덤 수 생성기에 대해 경쟁하는 상황을 피할 수 있기 때문입니다.
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
    }

    static void addProduct(String productName) {
        log("Adding product:" + productName);
    }

    static void notifyShop(String productName) {
        log("Notifying shop about:" + productName);
    }

    static void handleRequest() {
        log("hello1");
        initRequest();

        log("hello2");

        addProduct("test-product");
        notifyShop("test-product");
    }

    public static void main(String[] args) {
        handleRequest();
    }


}
