package replication.sync;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

import replication.ReplicationManager;

public class WaitRequestManager {

    private final PriorityBlockingQueue<WaitRequest> waitQueue = new PriorityBlockingQueue<>();
    private final ReplicationManager replicationManager;
    private final ReentrantLock signalLock = new ReentrantLock();

    public WaitRequestManager(ReplicationManager replicationManager) {
        this.replicationManager = replicationManager;
    }

    public int awaitClientForAcks(WaitRequest req) throws InterruptedException {
        req.getLock().lock();
        try {
            if (req.tryComplete()) {
                return req.getCurrentReceivedAcks();
            }
            waitQueue.add(req);
            replicationManager.askForOffsetAcksFromSlaves();

            if (req.tryComplete()) {
                waitQueue.remove(req);
                return req.getCurrentReceivedAcks();
            }

            long remaining = req.remainingNanos();
            while (!req.isFulfilled() && remaining > 0) {
                remaining = req.getCondition().awaitNanos(remaining);
            }

            waitQueue.remove(req);

            return req.getCurrentReceivedAcks();
        } finally {
            req.getLock().unlock();
        }
    }

    public void signalWaiters(int replicaId, long replicaOffset) {
        signalLock.lock();
        try {
            while (true) {
                WaitRequest req = waitQueue.peek();
                if (req == null) {
                    return;
                }

                if (req.getOffsetTarget() > replicaOffset) {
                    return;
                }
                
                req = waitQueue.poll();
                
                if (req == null) {
                    continue;
                }
                
                req.getLock().lock();
                try {
                    boolean isNewAck = req.ackFromReplica(replicaId);

                    if (!isNewAck) {
                        waitQueue.add(req);
                        return;
                    }

                    if (req.tryComplete()) {
                        req.getCondition().signal();
                        return;
                    } else {
                        waitQueue.add(req);
                        return;
                    }

                } finally {
                    req.getLock().unlock();
                }
            }
        } finally {
            signalLock.unlock();
        }
    }

    public void checkTimeouts() {
        for (WaitRequest req : waitQueue) {
            if (req.remainingNanos() <= 0) {
                req.getLock().lock();
                try {
                    if (!req.tryComplete()) {
                        req.getCondition().signal();
                    }
                    waitQueue.remove(req);
                } finally {
                    req.getLock().unlock();
                }
            }
        }
    }
}
