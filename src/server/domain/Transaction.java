package server.domain;

import java.util.Date;

// rappresenta la transazione su un wallet di un utente
public class Transaction {
    private double earn;  // valore del guadagno
    private Date timestamp;  // timestamp in cui è avvenuta la transazione

    public Transaction(double earn, Date timestamp){
        this.earn = earn;
        this.timestamp = timestamp;
    }


    public double getEarn() {
        return earn;
    }

    public Date getTimestamp() {
        return timestamp;
    }
}
