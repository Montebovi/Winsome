package shared.dto;

import java.util.Date;

// Data Transfer Object per le transazioni associate ai wallet
public class TransactionDto {
    private double earn;  // guadagno
    private Date timestamp;  // marca temporale della transazione

    public TransactionDto(double earn, Date timestamp){
        this.earn = earn;
        this.timestamp = timestamp;
    }

    public double getEarn() {
        return earn;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setEarn(double earn) {
        this.earn = earn;
    }
}
