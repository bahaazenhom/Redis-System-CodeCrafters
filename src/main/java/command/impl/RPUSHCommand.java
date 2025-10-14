package command.impl;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import command.CommandStrategy;
import protocol.RESPSerializer;
import storage.DataStore;
import storage.model.concreteValues.ListValue;

public class RPUSHCommand implements CommandStrategy {
    private final DataStore dataStore;

    public RPUSHCommand(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    public void execute(List<String> arguments, BufferedWriter clientOutput) {
        try{
        if(arguments.size()<2){
            clientOutput.write(RESPSerializer.error("wrong number of arguments for 'RPUSH' command"));
            clientOutput.flush();
        }
        String listName = arguments.get(0);
        List<String> values = arguments.subList(1, arguments.size());

        if(!dataStore.exists(listName))dataStore.setValue(listName, new ListValue(new ArrayList<>()));
        if(!(dataStore.getValue(listName) instanceof ListValue)){
            clientOutput.write(RESPSerializer.error("WRONGTYPE Operation against a key holding the wrong kind of value"));
            clientOutput.flush();
            return;
        }
        dataStore.addToList(listName, values);
        clientOutput.write(RESPSerializer.integer(((ListValue)dataStore.getValue(listName)).getList().size()));
        clientOutput.flush();
    }
    catch(IOException e){
        throw new RuntimeException(e);
    }
    }
    
}
