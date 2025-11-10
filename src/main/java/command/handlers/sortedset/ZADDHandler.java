package command.handlers.sortedset;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import command.CommandStrategy;
import protocol.RESPSerializer;
import server.connection.ClientConnection;
import storage.DataStore;
import domain.values.Member;

public class ZADDHandler implements CommandStrategy {
    private final DataStore dataStore;

    public ZADDHandler(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    public void execute(List<String> arguments, ClientConnection clientOutput) {
        try {
            String key = arguments.get(0);
            List<Member> members = parseMembers(arguments.subList(1, arguments.size()));
            int addedCount = dataStore.zadd(key, members);
            clientOutput.write(RESPSerializer.integer(addedCount));
            clientOutput.flush();
        } catch (IOException e) {
            throw new RuntimeException("IO Error during command execution", e);
        }
    }

    @Override
    public void validateArguments(List<String> arguments) throws IllegalArgumentException {
        if (arguments.size() < 3 || (arguments.size() - 1) % 2 != 0) {
            throw new IllegalArgumentException("ZADD command requires at least a key and one score-member pair.");
        }
    }

    private List<Member> parseMembers(List<String> args) throws IllegalArgumentException {
        List<Member> members = new ArrayList<>();
        for (int i = 0; i < args.size(); i += 2) {
            Member member = new Member(args.get(i + 1), Double.parseDouble(args.get(i)));
            members.add(member);
        }
        return members;
    }

}
