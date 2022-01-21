package shared;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

// classe cge rappresenta il comando inviato al server (in formato json)
public class Command {
    public static final int CMD_REGISTER = 0;
    public static final int CMD_LOGIN = 1;
    public static final int CMD_LOGOUT = 2;
    public static final int CMD_LIST_USERS = 3;
    public static final int CMD_LIST_FOLLOWERS = 4;
    public static final int CMD_LIST_FOLLOWING = 5;
    public static final int CMD_FOLLOW_USER = 6;
    public static final int CMD_UNFOLLOW_USER = 7;
    public static final int CMD_CREATE_POST = 8;
    public static final int CMD_VIEW_BLOG = 9;
    public static final int CMD_SHOW_FEED = 10;
    public static final int CMD_SHOW_POST = 11;
    public static final int CMD_DELETE_POST = 12;
    public static final int CMD_ADD_COMMENT = 13;
    public static final int CMD_RATE_POST = 14;
    public static final int CMD_REWIN_POST = 15;
    public static final int CMD_GET_WALLET = 16;
    public static final int CMD_GET_WALLETB = 17;

    public int code;  // codice del comando
    public ArrayList<String> parameters;  // parametri del comando

    public Command(int code, List<String> params){
        this.code = code;
        parameters = new ArrayList<>();
        parameters.addAll(params);
    }
}