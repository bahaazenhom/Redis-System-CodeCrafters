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
     * 
     * CRITICAL: Must be locked because multiple replica ACK threads
     * may call this concurrently for the same WaitRequest.
     */
    public void signalWaiters(int replicaId, long replicaOffset) {
        signalLock.lock();
        try {
            System.out.println("Signaling waiters for replica " + replicaId + " at offset " + replicaOffset);
            
            // Process all requests that this replica's offset satisfies
            while (true) {
                WaitRequest req = waitQueue.peek();
                if (req == null) {
                    System.out.println("[AcksWaitManager] No pending wait requests.");
                    return;
                }

                // If the first waiter requires higher offset than this replica reached, stop.
                if (req.getOffsetTarget() > replicaOffset) {
                    System.out.println("[AcksWaitManager] Replica offset " + replicaOffset + 
                        " < target offset " + req.getOffsetTarget() + ", stopping.");
                    return;
                }
                
                System.out.println(
                        "the replica offset " + replicaOffset + " reached the target offset " + req.getOffsetTarget());
                
                // Remove the request so we can update it
                req = waitQueue.poll();
                
                if (req == null) {
                    // Request was removed by another thread (shouldn't happen with lock)
                    continue;
                }
                
                req.getLock().lock();
                try {
                    // Count ACK only once per replica
                    // This returns false if this replica already ACK'd this request
                    boolean isNewAck = req.ackFromReplica(replicaId);

                    if (!isNewAck) {
                        // This replica already ACK'd this request, reinsert and stop
                        // (single ACK can't satisfy the same request multiple times)
                        System.out.println("Replica " + replicaId + " already ACK'd this request, reinserting");
                        waitQueue.add(req);
                        return; // CRITICAL: Stop processing, this ACK is done
                    }

                    // If request completed, wake it and DON'T reinsert
                    if (req.tryComplete()) {
                        System.out.println("Request fulfilled! Waking client thread.");
                        req.getCondition().signal();
                        // Don't reinsert - request is complete
                        return; // Exit after completing a request
                    } else {
                        System.out.println("Not enough replicas yet → reinsert request for future ACKs");
                        System.out.println("as the current acks are " + req.getCurrentReceivedAcks() + 
                            " and required acks are " + req.getNumAcksRequired());
                        // Not enough replicas yet → reinsert request for future ACKs
                        waitQueue.add(req);
                        // CRITICAL: After reinserting, stop processing this ACK
                        // Next ACK from a different replica will pick this up
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
