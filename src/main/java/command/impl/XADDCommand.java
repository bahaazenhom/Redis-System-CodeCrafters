package command.impl;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import command.CommandStrategy;
import protocol.RESPSerializer;
import storage.DataStore;
import storage.exception.InvalidStreamEntryException;

public class XADDCommand implements CommandStrategy {
    private final DataStore dataStore;

    public XADDCommand(DataStore dataStore){
        this.dataStore = dataStore;
    }

    @Override
    public void execute(List<String> arguments, BufferedWriter clientOutput) {
        try{
            if(arguments.size()<4){
                clientOutput.write(RESPSerializer.error("Wrong number of arguments of the 'XADD' command."));
                clientOutput.flush();
                return;
            }
            String streamKey = arguments.get(0);
            String entryID = arguments.get(1);
            HashMap<String, String> entryValues = new HashMap<>();
            for(int index = 2;index<arguments.size();index+=2){
                String key = arguments.get(index);
                String value = arguments.get(index+1);
                entryValues.put(key, value);
            }

            entryID = dataStore.xadd(streamKey, entryID, entryValues);
            clientOutput.write(RESPSerializer.bulkString(entryID));
            clientOutput.flush();
        }
        catch(InvalidStreamEntryException e){
            try {
                System.out.println(e.getMessage());
                clientOutput.write(RESPSerializer.error(e.getMessage()));
                clientOutput.flush();
            } catch (IOException ioException) {
                throw new RuntimeException(ioException);
            }
        }
        catch(IOException exception){
            throw new RuntimeException(exception);
        }
    }
    
}
