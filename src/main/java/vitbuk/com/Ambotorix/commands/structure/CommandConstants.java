package vitbuk.com.Ambotorix.commands.structure;

public final class CommandConstants {
    /*Commands dump:
    *  /settings - reminder which settings to change if u installed civ again
    * /mods - reminder which mods are necessary to add
    *
    * */

    //common prefix for all commands
    public static final String PREFIX = "/";

    //dynamic commands
    public static final String DESCRIPTION = "/d";
    public static final String DESCRIPTION_NAME = "/d_[shortName]";
    public static final String DESCRIPTION_NAME2 = "/d [shortName]";

    //commands for everyone
    public static final String HELP = "/help";
    public static final String LOBBY = "/lobby";
    public static final String LEADERS = "/leaders";
    public static final String MAPLIST = "/maplist";
    public static final String MAPPOLL = "/mappool";
    public static final String REGISTER = "/register";
    public static final String TIME = "/time";

    //non-dynamic commands for the host of the lobby
    public static final String START = "/start";
    public static final String RELOBBY = "/relobby";
    public static final String RESTART = "/restart";
    public static final String STANDARD = "/standard";

    //dynamic commands for the host of the lobby
    public static final String BANSIZE = "/bansize";
    public static final String BANSIZE_NAME = "/bansize [amount]";
    public static final String PICKSIZE = "/picksize";
    public static final String PICKSIZE_NAME = "/picksize [amount]";
    public static final String MAPADD = "/mapAdd";
    public static final String MAPADD_NAME = "/mapAdd [name]";
    public static final String MAPREMOVE = "/mapRemove";
    public static final String MAPREMOVE_NAME = "/mapRemove [name]";

    //dynamic commands for the registered players
    public static final String BAN = "/ban";
    public static final String BAN_NAME = "/ban [shortName]";
}
