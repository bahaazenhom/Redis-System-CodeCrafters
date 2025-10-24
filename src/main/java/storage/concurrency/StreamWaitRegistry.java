package storage.concurrency;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class StreamWaitRegistry {

    private final ConcurrentHashMap<String, KeyWaitQueue> streamWaitQueues = new ConcurrentHashMap<>();

    private static class KeyWaitQueue {
        private final ReentrantLock lock = new ReentrantLock(true);// true: to ensure fair FIFO ordering
        private final Condition condition = lock.newCondition();
        private final Queue<StreamWaitToken> waiters = new LinkedList<>();
    }

    private static class StreamWaitToken {
        private volatile boolean fulfilled = false;
        private volatile List<List<Object>> result = null;
        private String entryID = null;

        public String getEntryID() {
            return entryID;
        }

        public void setEntryID(String entryID) {
            this.entryID = entryID;
        }

        void fulfill(List<List<Object>> value) {
            this.result = value;
            this.fulfilled = true;
        }

        boolean isFulfilled() {
            return this.fulfilled;
        }

        List<List<Object>> getResult() {
            return this.result;
        }

    }

    public List<List<Object>> awaitElement(String key, String entryId, double timeoutSeconds,
            Supplier<List<List<Object>>> readSupplier)
            throws InterruptedException {
        KeyWaitQueue queue = streamWaitQueues.computeIfAbsent(key, k -> new KeyWaitQueue());
        StreamWaitToken token = new StreamWaitToken();
        token.setEntryID(entryId);

        queue.lock.lock();
        try {
            List<List<Object>> value = readSupplier.get();
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
                // Convert fractional seconds to nanoseconds
                long nanos = (long) (timeoutSeconds * 1_000_000_000);
                // wait for a signal or timeout reached
                while (!token.isFulfilled() && nanos > 0) {
                    nanos = queue.condition.awaitNanos(nanos);
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

    public void signalFirstWaiter(String key, String entryID,Supplier<List<List<Object>>> streamSupplier) {
       // System.out.println("Signaling waiters for stream " + key + " with entryID " + entryID);
        KeyWaitQueue queue = streamWaitQueues.get(key);
        System.out.println(queue);
        if (queue == null)
            return;// there's no any waiting clients
        queue.lock.lock();
        try {
            while (!queue.waiters.isEmpty()) {// we go through all the waiting clients to give them their values
                StreamWaitToken token = queue.waiters.peek();
                List<List<Object>> entries = streamSupplier.get();
                System.out.println("Checking waiter for stream " + key + " with entryID " + token.getEntryID()+"\n"+entries.toString());
                if (entries == null || entries.isEmpty()) {
                    return;// no more elements to read
                }
                if (entryID.compareTo(token.getEntryID()) < 0)
                    continue;// not the entryID we're looking for

                System.out.println("Signaling waiter for stream " + key + " with entryID " + token.getEntryID()+"\n"+entries.toString());
                queue.waiters.poll();
                token.fulfill(entries);

                queue.condition.signal(); // signal the most waiting client
            }
        } finally {
            queue.lock.unlock();
        }

    }
}