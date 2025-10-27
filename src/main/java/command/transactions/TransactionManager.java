package command.transactions;

import java.util.concurrent.ConcurrentHashMap;

import command.CommandRequest;

public class TransactionManager {
    private final ConcurrentHashMap<String, TransactionContext> contexts = new ConcurrentHashMap<>();


    public TransactionContext beginTransactionContext(String clientId) {
        return contexts.computeIfAbsent(clientId, id -> new TransactionContext());
    }

    public boolean isInMultiMode(String clientId) {
        TransactionContext context = contexts.get(clientId);
        return context != null && context.isInMultiMode();
    }

    public void enqueueCommand(String clientId, CommandRequest commandRequest) {
        contexts.computeIfAbsent(clientId, id -> new TransactionContext())
                .enqueueCommand(commandRequest);
    }
}
