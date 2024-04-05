package me.contextpropagation.contextpropagation;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
public class ReactorCoreFailDelay {

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


    static Mono<Void> addProduct(String productName) {
        log("Adding product:" + productName);
        return Mono.empty();
    }

    static Mono<Boolean> notifyShop(String productName) {
        log("Notifying shop about:" + productName);
        return Mono.just(true);
    }

    static Mono<Void> handleRequest() {
        initRequest();
        log("Assembling the chian.");

        // 이 경우는 문제 발생함.
        // delayElement()가 호출되면서, 뒤의 두 addProduct(), notifyShop()은 다른 쓰레드에서 실행됨.
        // 그런데 쓰레드간 상태를 전달하지 않았기 때문에 correlation ID는 전파되지 않음.
        return Mono.just("test-product")
                   .delayElement(Duration.ofMillis(10))
                   .flatMap(product ->
                                    Flux.concat(
                                                addProduct(product),
                                                notifyShop(product))
                                        .then());
    }

    public static void main(String[] args) throws InterruptedException {
        Mono<Void> voidMono = handleRequest();
        voidMono.subscribe();

        Thread.sleep(1000L);
    }


}
