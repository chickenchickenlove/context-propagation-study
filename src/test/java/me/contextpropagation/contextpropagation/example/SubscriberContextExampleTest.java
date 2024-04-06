package me.contextpropagation.contextpropagation.example;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class SubscriberContextExampleTest {


    // Context는 구독하는 시점에 생성됨.
    // 1. Context는 DownStream에서 Upstream으로 올라감.
    // 2. Context는 불변 객체임
    // 3.

    // Source Operator에서 Context에 접근하기 위해서deferContextual()을 사용해야 함.
    // Chaining 중간에서 Context에 접근하려면 transformDeferredContextual()을 사용해야 함.
    // Context에 값을 적기 위해서는 contextWrite)를 사용해야 함.
    @Test
    void test1() {
        String key = "message";
        Mono<String> r = Mono.just("Hello")
                             .flatMap(s -> Mono.deferContextual(ctx ->
                                                                        Mono.just(s + " " + ctx.get(key))))
                             .contextWrite(ctx -> ctx.put(key, "World"));

        StepVerifier
                .create(r)
                .expectNext("Hello World")
                .verifyComplete();
    }

    @Test
    void test2() {
        String key = "message";
        Flux<String> r = Flux.just("Hello", "Hi")
                             .flatMap(s -> Mono.deferContextual(ctx ->Mono.just(s + " " + ctx.get(key))))
                             .contextWrite(ctx -> ctx.put(key, "World"));

        StepVerifier
                .create(r)
                .expectNext("Hello World")
                .expectNext("Hi World")
                .verifyComplete();
    }

    @Test
    void test3() {
        String key = "message";
        Flux<String> r = Flux.just("Hello")
                             .flatMap(s -> Mono.deferContextual(ctx -> Mono.just(s + " " + ctx.get(key))))
                             .contextWrite(ctx -> ctx.put(key, "Sekai")) // 가장 최근의 값만 반영됨.
                             .contextWrite(ctx -> ctx.put(key, "World"));

        StepVerifier
                .create(r)
                .expectNext("Hello Sekai")
                .verifyComplete();
    }


    @Test
    void test4() {
        String key1 = "message1";
        String key2 = "message2";
        Flux<String> r = Flux.just("Hello")
                             .flatMap(s -> Mono.deferContextual(ctx -> Mono.just(s + " " + ctx.get(key1) +  " " + ctx.get(key2))))
                             .contextWrite(ctx -> ctx.put(key1, "Real")) // 가장 최근의 값만 반영됨.
                             .contextWrite(ctx -> ctx.put(key2, "World"));

        StepVerifier
                .create(r)
                .expectNext("Hello Real World")
                .verifyComplete();
    }


    // contextWrite()의 영향을 받지 못함.
    @Test
    void test5() {
        String key = "message";
        Mono<String> r = Mono.just("Hello")
                                      .contextWrite(ctx -> ctx.put(key, "World"))
                                      .flatMap(s -> Mono.deferContextual(
                                              ctx -> Mono.just(s + " " + ctx.getOrDefault(key, "Stranger"))));

        StepVerifier
                .create(r)
                .expectNext("Hello Stranger")
                .verifyComplete();
    }


    @Test
    void test6() {
        String key = "message";
        Mono<String> r = Mono.just("Hello")
                             .contextWrite(ctx -> ctx.put(key, "World"))
                             .flatMap(s -> Mono.deferContextual(
                                     ctx -> Mono.just(s + " " + ctx.getOrDefault(key, "Stranger"))))
                             .contextWrite(ctx -> ctx.put(key, "World2")) // 이거
                             .flatMap(s -> Mono.deferContextual(
                                     ctx -> Mono.just(s + " " + ctx.getOrDefault(key, "No")))); // 이거

        StepVerifier
                .create(r)
                .expectNext("Hello World2 No")
                .verifyComplete();
    }


    // Source로도 사용할 수 있음.
    @Test
    void test7() {
        String key = "message";
        Mono<String> r = Mono.deferContextual(ctx -> Mono.just("Hello" + " " + ctx.get(key)))
                             .delayElement(Duration.ofMillis(10))
                             .contextWrite(ctx -> ctx.put(key, "Reactor"))
                             .flatMap(s -> Mono.deferContextual(
                                     ctx -> Mono.just(s + " " + ctx.get(key))))
                             .contextWrite(ctx -> ctx.put(key, "World"));

        StepVerifier
                .create(r)
                .expectNext("Hello Reactor World")
                .verifyComplete();
    }
}
