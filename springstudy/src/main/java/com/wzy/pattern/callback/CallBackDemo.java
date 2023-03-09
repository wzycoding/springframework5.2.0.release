package com.wzy.pattern.callback;

public class CallBackDemo {

    public static void main(String[] args) {
        Thread t = new Thread(() -> {
            System.out.println("我要休息了");
            try {
                Thread.sleep(2000);
                System.out.println("我被回调了");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        t.start();
    }
}
