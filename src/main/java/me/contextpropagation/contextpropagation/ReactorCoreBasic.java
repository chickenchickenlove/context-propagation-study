package me.contextpropagation.contextpropagation;

import java.util.concurrent.ThreadLocalRandom;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
public class ReactorCoreBasic {

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

        // 이 경우는 문제 없음.
        // 같은 Main 쓰레드에서 모든 체인이 실행되기 때문임.
        return Mono.just("test-product")
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

    /*
    23:11:25.692 [main] INFO me.contextpropagation.contextpropagation.ReactorCoreFail -- [main][     1117662376929417040] init.
    23:11:25.694 [main] INFO me.contextpropagation.contextpropagation.ReactorCoreFail -- [main][     1117662376929417040] Assembling the chian.
    23:11:25.758 [main] INFO me.contextpropagation.contextpropagation.ReactorCoreFail -- [main][     1117662376929417040] Adding product:test-product
    23:11:25.759 [main] INFO me.contextpropagation.contextpropagation.ReactorCoreFail -- [main][     1117662376929417040] Notifying shop about:test-product
     */
}
