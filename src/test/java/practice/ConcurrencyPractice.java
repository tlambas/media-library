package practice;

import java.util.ArrayList;

public class ConcurrencyPractice {
    public static void main(String[] args) throws InterruptedException {
        Counter counter = new Counter();

        ArrayList<Integer> ints = new ArrayList<>();

        Thread thread1 = new Thread(() -> {
            for (int i = 0; i < 100000; i++) {
                counter.increment();
            }
        });

        Thread thread2 = new Thread(() -> {
            for (int i = 0; i < 100000; i++) {
                counter.increment();
            }
        });

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        System.out.println("Final count: " + counter.getCount());
    }
}
