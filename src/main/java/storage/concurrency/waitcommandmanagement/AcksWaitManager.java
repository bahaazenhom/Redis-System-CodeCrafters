package storage.concurrency.waitcommandmanagement;

import java.util.concurrent.PriorityBlockingQueue;

import replication.ReplicationManager;

public class AcksWaitManager {

    // Ordered by offsetTarget (lowest first)
    private final PriorityBlockingQueue<WaitRequest> waitQueue = new PriorityBlockingQueue<>();
    private final ReplicationManager replicationManager;

    public AcksWaitManager(ReplicationManager replicationManager) {
        this.replicationManager = replicationManager;
    }

    /**
     * Called by a client thread that issued WAIT.
     * Blocks this client until enough replica ACKs or timeout.
     */
    public int awaitClientForAcks(WaitRequest req) throws InterruptedException {
        System.out.println("client is waiting-------------");
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
     */
    public void signalWaiters(int replicaId, long replicaOffset) {
        System.out.println("Signaling waiters for replica " + replicaId + " at offset " + replicaOffset);
        while (true) {
            WaitRequest req = waitQueue.peek();
            if (req == null) {
                System.out.println("[AcksWaitManager] No pending wait requests.");
                return;
            }

            // If the first waiter requires higher offset than this replica reached, stop.
            if (req.getOffsetTarget() > replicaOffset)
                return;
            System.out.println(
                    "the replica offset " + replicaOffset + " reached the target offset " + req.getOffsetTarget());
            // Remove the request so we can update it
            req = waitQueue.poll();
            req.getLock().lock();
            try {
                // Count ACK only once per replica
                req.ackFromReplica(replicaId);

                // If request completed, wake it
                if (req.tryComplete()) {
                    req.getCondition().signal();
                } else {
                    System.out.println("Not enough replicas yet → reinsert request for future ACKs");
                    // Not enough replicas yet → reinsert request for future ACKs
                    waitQueue.add(req);
                }

            } finally {
                req.getLock().unlock();
            }
        }
    }

    /**
     * Called periodically by a timeout sweeper thread (if you build one).
     * Wakes clients whose time expired.
     */
    public void checkTimeouts() {
        long now = System.nanoTime();
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
