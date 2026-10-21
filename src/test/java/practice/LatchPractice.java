package practice;

import java.util.concurrent.CountDownLatch;

public class LatchPractice {
    public static void main(String[] args) throws InterruptedException {
        CountDownLatch bothLoaded = new CountDownLatch(2);
        CountDownLatch firstCommitted = new CountDownLatch(1);

        Thread threadA = new Thread(() -> {
            try {
                System.out.println("A loaded item");
                bothLoaded.countDown();
                bothLoaded.await();
                System.out.println("A committed update");
                firstCommitted.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread threadB = new Thread(() -> {
            try {
                System.out.println("B loaded item");
                bothLoaded.countDown();
                bothLoaded.await();
                firstCommitted.await();
                System.out.println("B attempting stale update");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

        });

        threadA.start();
        threadB.start();

        threadA.join();
        threadB.join();
    }
}