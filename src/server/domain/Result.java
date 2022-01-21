package server.domain;

// classe utilizzata internamente per ritornare oggetti e codici di errore.
public class Result<T> {

    private final int resultCode;
    private final T payload;

    public Result(T payload, int resultCode){
        this.payload = payload;
        this.resultCode = resultCode;
    }

    public int getResultCode() {
        return resultCode;
    }

    public T getPayload() {
        return payload;
    }
}
