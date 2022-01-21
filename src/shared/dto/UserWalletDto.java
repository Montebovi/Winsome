package shared.dto;

import java.util.ArrayList;
import java.util.Collection;

// Data Trabsfer Object del wallet.
// Viene usato sempre lo stesso dto per il wallet in bitcoins. Per questo è presente il
// field appliedRate e il metodo isBitcoinCurrency
public class UserWalletDto {
    private final int idUser;
    private final double totalCoins;   // totale coins (in wincoins se isBitcoinCurrency=false; in bitcoins se isBitcoinCurrency=true)
    private boolean isBitcoinCurrency;  //true => è in bitcons; false => è in wincoins
    private final double appliedRate;  // tasso applicato da wincoins a bitcoins (utile quando isBitcoinCurrency=true)
    private ArrayList<TransactionDto> transactions;  // elenco di transazioni

    public UserWalletDto(int idUser, double totalCoins, Collection<TransactionDto> transactions, boolean bitcoinCurrency, double tasso){
        this.idUser = idUser;
        this.totalCoins = totalCoins;
        this.transactions = new ArrayList<>(transactions);
        this.isBitcoinCurrency = bitcoinCurrency;
        this.appliedRate = tasso;
    }

    public double getTotalCoins() {
        return totalCoins;
    }

    // ritorna tutte le transazioni associate al wallet
    public ArrayList<TransactionDto> getTransactions() {
        return transactions;
    }

    // indica che il wallet è in bitcoins
    public boolean isBitcoinCurrency() {
        return isBitcoinCurrency;
    }

    // ritorna tasso applicato
    public double getAppliedRate() {
        return appliedRate;
    }
}
