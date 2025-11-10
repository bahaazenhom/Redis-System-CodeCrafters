package command.handlers.connection;

import java.io.IOException;
import java.util.List;

import command.CommandStrategy;
import protocol.RESPSerializer;
import server.connection.ClientConnection;
import server.core.ServerContext;
import util.ServerConfiguration;

/**
 * CONFIG GET command implementation.
 * Returns configuration parameters.
 * 
 * Usage: CONFIG GET <parameter>
 * Example: CONFIG GET dir
 */
public class CONFIGHandler implements CommandStrategy {

    @Override
    public void execute(List<String> arguments, ClientConnection clientOutput) {
        try {
            String subCommand = arguments.get(0).toUpperCase();
            if (subCommand.equals("GET")) {
                handleConfigGet(arguments.get(1), clientOutput);
            } else {
                clientOutput.write(RESPSerializer.error("ERR Unsupported CONFIG subcommand"));
                clientOutput.flush();
            }
        } catch (IOException e) {
            throw new RuntimeException("Error executing CONFIG command", e);
        }
    }

    private void handleConfigGet(String parameter, ClientConnection clientOutput) throws IOException {
        ServerConfiguration config = ServerContext.getInstance().getConfiguration();
        if (config == null) {
            clientOutput.write(RESPSerializer.error("ERR server configuration not available"));
            clientOutput.flush();
            return;
        }

        parameter = parameter.toLowerCase();
        String[] result;

        switch (parameter) {
            case "dir":
                String dir = config.getRdbFileDir();
                result = dir != null ? new String[] { "dir", dir } : new String[] {};
                break;

            case "dbfilename":
                String filename = config.getRdbFileName();
                result = filename != null ? new String[] { "dbfilename", filename } : new String[] {};
                break;

            case "port":
                result = new String[] { "port", String.valueOf(config.getPort()) };
                break;

            case "replicaof":
                if (config.isSlave()) {
                    result = new String[] { "replicaof", config.getMasterHost() + " " + config.getMasterPort() };
                } else {
                    result = new String[] {};
                }
                break;

            default:
                // Parameter not found - return empty array
                result = new String[] {};
                break;
        }

        clientOutput.write(RESPSerializer.array(List.of(result)));
        clientOutput.flush();
    }

    @Override
    public void validateArguments(List<String> arguments) throws IllegalArgumentException {
        if (arguments.size() < 2) {
            throw new IllegalArgumentException("Wrong number of arguments for 'CONFIG' command");
        }
    }

}
