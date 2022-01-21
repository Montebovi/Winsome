package shared.utils;

// eccezione usata durante la lettura del config
public class IncorrectLineConfigException extends Exception {
    public IncorrectLineConfigException(String errorMessage) {
        super(errorMessage);
    }
}
