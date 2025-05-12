package vitbuk.com.Ambotorix.commands.structure;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CommandFactory {
    private final Map<String, Command> commandMap = new HashMap<>();
    private final Map<Class<? extends Command>, Command> typeMap = new HashMap<>();

    public CommandFactory(List<Command> commands) {
        for (Command c : commands) {
            commandMap.put(c.getInfo().prefix(), c);
            typeMap.put(c.getClass(), c);
        }
    }

    public Command getCommand(String commandPrefix){
        System.out.println("c: [" + commandMap.get(commandPrefix) + "]");
        return commandMap.get(commandPrefix);
    }

    public List<Command> getAll() {
        return new ArrayList<>(commandMap.values());
    }

    public <T extends Command> CommandInfo infoOf(Class<T> type) {
        Command command = typeMap.get(type);
        if (command == null) {
            throw new IllegalArgumentException("No command registered for " + type.getSimpleName());
        }
        return command.getInfo();
    }
}