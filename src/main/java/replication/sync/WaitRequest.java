package replication.sync;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;


public class WaitRequest implements Comparable<WaitRequest> {
    private final long offsetTarget;              
    private final int numAcksRequired;
    private final long timeoutNanos;            

    private final ReentrantLock lock = new ReentrantLock(true);
    private final Condition condition = lock.newCondition();

    private final Set<Integer> ackedReplicas = new HashSet<>();
    private int currentReceivedAcks = 0;
    private boolean completed = false;

    public WaitRequest(long offsetTarget, int numAcksRequired, long timeoutMillis) {
        this.offsetTarget = offsetTarget;
        this.numAcksRequired = numAcksRequired;
        this.timeoutNanos = timeoutMillis <= 0 ? Long.MAX_VALUE
                                               : System.nanoTime() + (timeoutMillis * 1_000_000);
    }

    public boolean ackFromReplica(int replicaId) {
        if (ackedReplicas.add(replicaId)) {
            currentReceivedAcks++;
            return true;
        }
        return false;
    }

    public boolean isFulfilled() {
        return currentReceivedAcks >= numAcksRequired;
    }

    public boolean tryComplete() {
        if (!completed && isFulfilled()) {
            completed = true;
            return true;
        }
        return false;
    }

    public long remainingNanos() {
        long now = System.nanoTime();
        return timeoutNanos - now;
    }

    @Override
    public int compareTo(WaitRequest o) {
        return Long.compare(this.offsetTarget, o.offsetTarget);
    }

    public long getOffsetTarget() { return offsetTarget; }
    public ReentrantLock getLock() { return lock; }
    public Condition getCondition() { return condition; }
    public int getCurrentReceivedAcks() { return currentReceivedAcks; }

    public int getNumAcksRequired() {
        return numAcksRequired;
    }
}
