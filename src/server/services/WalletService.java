package server.services;

import server.managers.WalletManager;
import shared.utils.AppLogger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

// classe per gestire il calcolo periodico dei wallet con notifica di avvenuto aggiornamto in multicast ai client
public class WalletService {
    private final long periodMs;
    private final long delayMs;
    private final WalletManager walletManager;

    Timer timer;
    TimerTask timerTask;

    int multicatsPort;
    String multicastAddress;
    DatagramSocket dgSocket;
    InetAddress group;

    public WalletService(String multicastAddress, int multicatsPort,  WalletManager walletManager, long periodInSec){
        this.multicatsPort = multicatsPort;
        this.multicastAddress =multicastAddress;
        this.walletManager = walletManager;
        this.delayMs = periodInSec*1000;
        this.periodMs = periodInSec*1000;
    }

    // avvio del task e apertura del socket
    public void start(){
        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                updateWallets();
            }
        };
        timer.schedule(timerTask, delayMs, periodMs);

        startSocket();
    }

    // apertura del DatagramSocket per il multicast
    private void startSocket() {
        try  {
            group = InetAddress.getByName(multicastAddress);
            if (!group.isMulticastAddress()) {
                throw new IllegalArgumentException(
                        "Indirizzo multicast non valido: " + group.getHostAddress());
            }
            dgSocket = new DatagramSocket();
        }
        catch (Exception exc) {
            AppLogger.log("Errore server: " + exc.getMessage());
        }
    }

    // stop del servizio: close del socket, disattivazione del timer
    public void stop(){
        if (dgSocket != null)
        {
            dgSocket.close();
            dgSocket = null;
        }
        if (timerTask != null)
          timerTask.cancel();
        if (timer != null)
        {
            timer.cancel();
            timer.purge();
            timer = null;
        }
    }

    // aggiornamento dei wallets
    private void updateWallets() {
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy.MM.dd.HH:mm:ss");
        String timeStamp = dateFormatter.format(new Date());
        System.out.println("update wallets: "+timeStamp);
        walletManager.calculateWallets(); // aggiornamento wallet

        sendUpdateToClients();  // notifica ai client
    }

    // invio notifica ai client
    private void sendUpdateToClients() {
        String message = "wallets update";
        byte[] content = message.getBytes();
        DatagramPacket packet = new DatagramPacket(content, content.length, group, multicatsPort);
        // Invio il pacchetto.
        try {
            dgSocket.send(packet);
        } catch (IOException e) {
            AppLogger.log("Errore: "+e.getMessage());
            e.printStackTrace();
        }
    }
}
