package command.impl;

import java.io.IOException;

import command.CommandStrategy;
import command.transactions.TransactionManager;
import protocol.RESPSerializer;

public class MULTICommand implements CommandStrategy {

    private final TransactionManager transactionManager;
    public MULTICommand(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }
    @Override
    public void validateArguments(java.util.List<String> arguments) throws IllegalArgumentException {
        if (arguments.size() != 1) {
            throw new IllegalArgumentException("Wrong number of arguments for 'MULTI' command");
        }
    }

    @Override
    public void execute(java.util.List<String> arguments, java.io.BufferedWriter clientOutput) {
        String clientId = arguments.get(0);
        transactionManager.beginTransactionContext(clientId);
        try{
        clientOutput.write(RESPSerializer.simpleString("OK"));
        clientOutput.flush();
    } catch (IOException e) {
        throw new RuntimeException(e);
    }
}
}