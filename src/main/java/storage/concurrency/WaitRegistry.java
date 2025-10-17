package storage.concurrency;

import java.security.Key;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class WaitRegistry {

    private final ConcurrentHashMap<String, KeyWaitQueue> waitQueues = new ConcurrentHashMap<>();

    private static class KeyWaitQueue {
        private final ReentrantLock lock = new ReentrantLock(true);// true: to ensure fair FIFO ordering
        private final Condition condition = lock.newCondition();
        private final Queue<WaitToken> waiters = new LinkedList<>();
    }

    private static class WaitToken {
        private volatile boolean fulfilled = false;
        private volatile String result = null;

        void fulfill(String value) {
            this.result = value;
            this.fulfilled = true;
        }

        boolean isFulfilled() {
            return this.fulfilled;
        }

        String getResult() {
            return this.result;
        }

    }

    public String awaitElement(String key, Long timeoutSeconds, Supplier<String> popSupplier)
            throws InterruptedException {
        KeyWaitQueue queue = waitQueues.computeIfAbsent(key, k -> new KeyWaitQueue());
        WaitToken token = new WaitToken();

        queue.lock.lock();
        try {
            String value = popSupplier.get();
            if (value != null)
                return value;// double checking before waiting

            // add client to the waiting list
            queue.waiters.add(token);

            if (timeoutSeconds == 0) {
                while (!token.isFulfilled()) {
                    // wait forever for a signal (as the timeout is 0)
                    queue.condition.await();// the client sleep and the lock is released
                }
            } else {
                long nano = TimeUnit.SECONDS.toNanos(timeoutSeconds);
                // wait for a signal or timeout reaced
                while (!token.isFulfilled() && nano > 0) {
                    nano = queue.condition.awaitNanos(nano);
                }
            }
            // some pushed and we got a signal
            if (token.isFulfilled())
                return token.getResult();
            else {
                // timeout
                queue.waiters.remove(token);
                return null;
            }
        } finally {
            queue.lock.unlock();
        }

    }

    public void signalFirstWaiter(String key, Supplier<String> popSupplier) {
        KeyWaitQueue queue = waitQueues.get(key);
        if (queue == null)
            return;// there's no any waiting clients
        queue.lock.lock();
        try {
            while (!queue.waiters.isEmpty()) {// we go through all the waiting clients to give them their values
                WaitToken token = queue.waiters.poll();
                String value = popSupplier.get();
                if (value == null)
                    return;// no more elements to pop
                token.fulfill(value);//

                queue.condition.signal();// signal the most waiting client
            }
        } finally {
            queue.lock.unlock();
        }

    }
}