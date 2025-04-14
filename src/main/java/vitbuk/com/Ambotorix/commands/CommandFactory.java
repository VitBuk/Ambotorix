package vitbuk.com.Ambotorix.commands;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandFactory {
    private final Map<String,Command> commandMap = new HashMap<>();

    public CommandFactory(List<Command> commands) {
        for (Command c : commands) {
            commandMap.put(c.getCommandName(), c);
        }
    }
    
    public Command getCommand(String commandName){
        return commandMap.get(commandName);
    }
}
