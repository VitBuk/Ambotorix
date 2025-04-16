package vitbuk.com.Ambotorix.commands.structure;

public final class CommandConstants {
    //common prefix for all commands
    public static final String PREFIX = "/";

    //dynamic commands
    public static final String DESCRIPTION = "/d_";
    public static final String DESCRIPTION_NAME = "/d_[shortName]";

    //commands for everyone
    public static final String HELP = "/help";
    public static final String LOBBY = "/lobby";
    public static final String LEADERS = "/leaders";
    public static final String REGISTER = "/register";
    public static final String TIME = "/time";

    //non-dynamic commands for the host of the lobby
    public static final String RELOBBY = "/relobby";
    public static final String RESTART = "/restart";
    public static final String STANDARD = "/standard";

    //dynamic commands for the host of the lobby
    public static final String BANSIZE = "/bansize";
    public static final String BANSIZE_NAME = "/bansize [amount]";
    public static final String PICKSIZE = "/picksize";
    public static final String PICKSIZE_NAME = "/picksize [amount]";
    public static final String ADDMAP = "/addMap";
    public static final String ADDMAP_NAME = "/addMap [amount]";
    public static final String REMOVEMAP = "/removeMap [name]";
    public static final String REMOVEMAP_NAME = "/removeMap [name]";

    //dynamic commands for the registered players
    public static final String BAN = "/ban_";
    public static final String BAN_NAME = "/ban_[shortName]";
}
