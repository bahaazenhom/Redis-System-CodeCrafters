package storage.concurrency.waitcommandmanagement;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

import replication.ReplicationManager;

public class AcksWaitManager {

    // Ordered by offsetTarget (lowest first)
    private final PriorityBlockingQueue<WaitRequest> waitQueue = new PriorityBlockingQueue<>();
    private final ReplicationManager replicationManager;
    private final ReentrantLock signalLock = new ReentrantLock(); // Protects signalWaiters from concurrent calls

    public AcksWaitManager(ReplicationManager replicationManager) {
        this.replicationManager = replicationManager;
    }

    /**
     * Called by a client thread that issued WAIT.
     * Blocks this client until enough replica ACKs or timeout.
     */
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

            // Remove from queue (idempotent, if already gone no issue)
            waitQueue.remove(req);

            return req.getCurrentReceivedAcks();
        } finally {
            req.getLock().unlock();
        }
    }

    /**
     * Called when a replica acknowledges an offset.
     * replicaId identifies which replica, replicaOffset = the offset the replica
     * reached.
     * 
     * CRITICAL: Must be locked because multiple replica ACK threads
     * may call this concurrently for the same WaitRequest.
     */
    public void signalWaiters(int replicaId, long replicaOffset) {
        signalLock.lock();
        try {
            // Process all requests that this replica's offset satisfies
            while (true) {
                WaitRequest req = waitQueue.peek();
                if (req == null) {
                    return;
                }

                // If the first waiter requires higher offset than this replica reached, stop.
                if (req.getOffsetTarget() > replicaOffset) {
                    return;
                }
                
                // Remove the request so we can update it
                req = waitQueue.poll();
                
                if (req == null) {
                    continue;
                }
                
                req.getLock().lock();
                try {
                    // Count ACK only once per replica
                    boolean isNewAck = req.ackFromReplica(replicaId);

                    if (!isNewAck) {
                        // This replica already ACK'd this request, reinsert and stop
                        waitQueue.add(req);
                        return;
                    }

                    // If request completed, wake it and DON'T reinsert
                    if (req.tryComplete()) {
                        req.getCondition().signal();
                        return;
                    } else {
                        // Not enough replicas yet → reinsert request for future ACKs
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

    /**
     * Called periodically by a timeout sweeper thread (if you build one).
     * Wakes clients whose time expired.
     */
    public void checkTimeouts() {
        for (WaitRequest req : waitQueue) {
            if (req.remainingNanos() <= 0) {
                req.getLock().lock();
                try {
                    if (!req.tryComplete()) {
                        // Timeout triggers completion too (client gets current acks)
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
