package vitbuk.com.Ambotorix.commands.structure;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CommandFactory {
    private final Map<String, Command> commandMap = new HashMap<>();

    public CommandFactory(List<Command> commands) {
        for (Command c : commands) {
            commandMap.put(c.getInfo().prefix(), c);
        }
    }

    public Command getCommand(String commandPrefix){
        System.out.println("c: [" + commandMap.get(commandPrefix) + "]");
        return commandMap.get(commandPrefix);
    }

    public List<Command> getAll() {
        return new ArrayList<>(commandMap.values());
    }
}