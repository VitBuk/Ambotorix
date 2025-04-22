package vitbuk.com.Ambotorix.commands.structure;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CommandFactory {
    private final Map<String, Command> commandMap = new HashMap<>();

    public CommandFactory(List<Command> commands) {
        for (Command c : commands) {
            commandMap.put(c.getPrefix(), c);
        }
    }

    public Command getCommand(String commandPrefix){
        System.out.println("c: [" + commandMap.get(commandPrefix) + "]");
        return commandMap.get(commandPrefix);
    }

    public void alLCommands() {
        System.out.println("keys:");
        for (String s : commandMap.keySet()) {
            System.out.println(s);
        }
        System.out.println("-------");
        System.out.println("values:");
        for (Command c : commandMap.values()) {
            System.out.println(c.getPrefix());
        }

    }
}