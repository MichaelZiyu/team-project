package game;

import java.util.concurrent.CountDownLatch;

public class PanelInput {

    private CountDownLatch latch;
    private String input;

    public synchronized void reset() {
        input = null;
        latch = new CountDownLatch(1);
    }

    public synchronized void submit(String input) {
        if (latch != null && latch.getCount() > 0) {
            this.input = input;
            latch.countDown();
        }
    }

    public String awaitInput() {
        CountDownLatch currentLatch;

        synchronized (this) {
            currentLatch = latch;
        }

        if (currentLatch == null) {
            throw new IllegalStateException("Call reset() first");
        }

        try {
            currentLatch.await();
            synchronized (this) {
                return input;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
}