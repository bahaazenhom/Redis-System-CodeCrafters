package command;

import java.util.List;

public class CommandRequest {
    private final String commandName;
    private final List<String> arguments;

    public CommandRequest(String commandName, List<String> arguments) {
        this.commandName = commandName;
        this.arguments = arguments;
    }

    public String getCommandName() {
        return commandName;
    }

    public List<String> getArguments() {
        return arguments;
    }
    
}
