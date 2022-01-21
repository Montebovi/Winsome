package shared.utils;

// codici di ritorno per le response dal server
public class ServiceResultCodes {
    public static final int SUCCESS = 0;
    public static final int ERR_UNKNOWN = -1;
    public static final int ERR_INVALID_USERNAME = -3;
    public static final int ERR_USERNAME_USED = -4;
    public static final int ERR_PASSWORD_TOOSHORT = -5;
    public static final int ERR_TOO_MUCH_TAGS = -6;
    public static final int ERR_WRONG_PWD = -7;
    public static final int ERR_USERNAME_NOT_EXISTING = -8;
    public static final int ERR_ALREADY_FOLLOWERS = -9;
    public static final int ERR_WRONG_NUM_PARAMS = -10;
    public static final int ERR_USER_NOT_FOLLOWED = -11;
    public static final int ERR_CONTENT_TOO_LONG = -12;
    public static final int ERR_TITLE_TOO_LONG = -13;
    public static final int ERR_IDPOST_NOT_NUMERIC = -14;
    public static final int ERR_POST_NOT_FOUND = -15;
    public static final int ERR_ACTION_DENIED = -16;
    public static final int ERR_POST_NOT_ACCESSIBLE = -17;
    public static final int ERR_AUTHOR_NOT_FOUND = -18;
    public static final int ERR_VOTE_NOT_CORRECT = -19;
    public static final int ERR_BITCOINS_RATE_NOT_AVAILABLE = -20;
}
