package com.example.asmbytecode.simpledemo;

public class InjectTest {

    public void sayHello() {
        long l = System.currentTimeMillis();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long cost = System.currentTimeMillis() - l;
        System.out.println("The cost time of " + cost + "ms");
    }
}
