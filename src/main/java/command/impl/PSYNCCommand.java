package command.impl;

import java.io.BufferedWriter;
import java.util.List;

import command.CommandStrategy;

public class PSYNCCommand implements CommandStrategy {

    @Override
    public void execute(List<String> arguments, BufferedWriter clientOutput) {
        try {
            clientOutput.write("+FULLRESYNC <REPL_ID> 0\r\n");
            clientOutput.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void validateArguments(List<String> arguments) throws IllegalArgumentException {
        if (arguments.size() != 2) {
            throw new IllegalArgumentException("Wrong number of arguments for 'PSYNC' command");
        }
    }

}
