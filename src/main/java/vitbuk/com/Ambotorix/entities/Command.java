package vitbuk.com.Ambotorix.entities;

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
}