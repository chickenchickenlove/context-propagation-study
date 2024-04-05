package me.contextpropagation.contextpropagation;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

/**
 * 쓰레드 로컬을 사용하지 않음.
 */

@Slf4j
public class ReactorCoreContext {

    static long correlationId() {
        return Math.abs(ThreadLocalRandom.current().nextLong());
    }

    static void log(String message, long correlationId) {
        String threadName = Thread.currentThread().getName();
        String threadNameTail = threadName.substring(
                Math.max(0, threadName.length() - 10));

        log.info("[{}][     {}] {}",
                 threadNameTail,
                 correlationId,
                 message);
    }

    static Mono<Void> addProduct(String productName) {
        return Mono.deferContextual(ctx -> {
            log("Adding product:" + productName, ctx.get("CORRELATION_ID"));
            return Mono.empty();
        });
    }

    static Mono<Boolean> notifyShop(String productName) {
        return Mono.deferContextual(ctx -> {
            log("Notifying shop about:" + productName,
                ctx.get("CORRELATION_ID"));
            return Mono.just(true);
        });
    }

    static Mono<Void> handleRequest() {
        long correlationId = correlationId();
        log("Assembling the chian.", correlationId);

        return Mono.just("test-product")
                   .delayElement(Duration.ofMillis(10))
                   .flatMap(product ->
                                    Flux.concat(
                                                addProduct(product),
                                                notifyShop(product))
                                        .then())
                   .contextWrite(Context.of("CORRELATION_ID", correlationId));
    }

    public static void main(String[] args) throws InterruptedException {
        Mono<Void> voidMono = handleRequest();
        voidMono.subscribe();

        Thread.sleep(1000L);
    }


}
