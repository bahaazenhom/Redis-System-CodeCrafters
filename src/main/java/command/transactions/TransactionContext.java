package command.transactions;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import command.CommandRequest;

public class TransactionContext {
    private final Queue<CommandRequest> commandQueue = new LinkedList<>();
    private boolean inMultiMode = true;

    public synchronized void enqueueCommand(CommandRequest commandRequest) {
        commandQueue.add(commandRequest);
    }

    public synchronized List<CommandRequest> drainCommands() {
        List<CommandRequest> commands = new LinkedList<>(commandQueue);
        commandQueue.clear();
        return commands;
    }

    public synchronized void discard(){
        commandQueue.clear();
        inMultiMode = false;
    }

    public synchronized boolean isInMultiMode() {
        return inMultiMode;
    }

    public synchronized void setInMultiMode(boolean inMultiMode) {
        this.inMultiMode = inMultiMode;
    }
}
