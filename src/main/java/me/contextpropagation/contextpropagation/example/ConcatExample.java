package me.contextpropagation.contextpropagation.example;

import java.time.Duration;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Slf4j
public class ConcatExample {

    public static void main(String[] args) throws InterruptedException {

        Flux<Integer> one = Flux.just(1, 2, 3, 4);
        Flux<Integer> two = Flux.just(10, 20, 30, 40);
        Flux<Integer> three = Flux.just(100, 200, 300, 400);

        Flux<Integer> f1 = Flux.concat(three, two, one)
                                        .delayElements(Duration.ofMillis(1));

        // 100 -> 200 -> 300 -> 400 -> 10 -> 20 -> 30 -> 40 -> 1 -> 2 -> 3 -> 4
        f1.subscribe(integer -> log.info("{}", integer));
        Thread.sleep(1000);

        System.out.println("=======");

        Flux<Integer> f2 = Flux.concat(two, one, three)
                                        .delayElements(Duration.ofMillis(1));

        // 10 -> 20 -> 30 -> 40 -> 1 -> 2-> 3 -> 4 -> 100 -> 200 -> 300 -> 400
        f2.subscribe(integer -> log.info("{}", integer));
        Thread.sleep(1000);

    }
}
// subscribe를 했을 때, 외부 쓰레드에서 처리됨.
/*
        08:14:42.815 [parallel-3] INFO me.contextpropagation.contextpropagation.example.ConcatExample -- 10
        08:14:42.817 [parallel-4] INFO me.contextpropagation.contextpropagation.example.ConcatExample -- 20
        08:14:42.818 [parallel-5] INFO me.contextpropagation.contextpropagation.example.ConcatExample -- 30
        08:14:42.820 [parallel-6] INFO me.contextpropagation.contextpropagation.example.ConcatExample -- 40
        08:14:42.821 [parallel-7] INFO me.contextpropagation.contextpropagation.example.ConcatExample -- 1
        08:14:42.823 [parallel-8] INFO me.contextpropagation.contextpropagation.example.ConcatExample -- 2
        08:14:42.824 [parallel-9] INFO me.contextpropagation.contextpropagation.example.ConcatExample -- 3
        08:14:42.826 [parallel-10] INFO me.contextpropagation.contextpropagation.example.ConcatExample -- 4
        08:14:42.827 [parallel-1] INFO me.contextpropagation.contextpropagation.example.ConcatExample -- 100
        08:14:42.828 [parallel-2] INFO me.contextpropagation.contextpropagation.example.ConcatExample -- 200
        08:14:42.830 [parallel-3] INFO me.contextpropagation.contextpropagation.example.ConcatExample -- 300
        08:14:42.831 [parallel-4] INFO me.contextpropagation.contextpropagation.example.ConcatExample -- 400
 */
