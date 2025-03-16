package vitbuk.com.Ambotorix.entities;

import java.util.Arrays;
import java.util.Optional;

public enum Command {
    LOBBY("/lobby"),
    LEADERS("/leaders");

    private final String commandText;

    Command(String commandText) {
        this.commandText = commandText;
    }

    public String getCommandText() {
        return commandText;
    }

    public static Optional<Command> fromCommandText(String commandText) {
        return Arrays.stream(Command.values())
                .filter(cmd -> cmd.getCommandText().equalsIgnoreCase(commandText))
                .findFirst();
    }
}