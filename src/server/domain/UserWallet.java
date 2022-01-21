package server.domain;

import java.util.ArrayList;

// classe che rappresenta un wallet
public class UserWallet {
    private int idUser;    // ide dell'utente proprietario del wallet
    private double totalWincoins;   // total wincoins
    private ArrayList<Transaction> transactions;  // elenco di transazioni

    public UserWallet(int idUser){
        this.idUser = idUser;
        totalWincoins = 0;
        transactions = new ArrayList<>();
    }

    public int getIdUser() {
        return idUser;
    }

    public double getTotalWincoins() {
        return totalWincoins;
    }

    public ArrayList<Transaction> getTransactions() {
        return transactions;
    }

    // aggiunge una transazione, aggiornando il totale
    public void addTransaction(Transaction transaction) {
        totalWincoins += transaction.getEarn();
        transactions.add(transaction);
    }
}
