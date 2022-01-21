package client;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

// thread per ricevere notifiche di avvenuto ricalcolo wallet (dal server).
public class WalletClientThread extends Thread {

    final int multicatsPort ;//= 44444;
    final String multicastAddress ;//= "239.255.32.32";
    public static final int size = 1024;

    public WalletClientThread(String multicastAddress, int multicatsPort){
        this.multicastAddress = multicastAddress;
        this.multicatsPort = multicatsPort;
    }

    // parte esecutiva del thread
    public void run() {
        try (MulticastSocket socket = new MulticastSocket(multicatsPort)) {
            InetAddress group = InetAddress.getByName(multicastAddress);
            if (!group.isMulticastAddress()) {
                throw new IllegalArgumentException(
                        "Indirizzo multicast non valido: " + group.getHostAddress());
            }
            // Mi unisco al gruppo multicast.
            socket.joinGroup(group);

            while (!isInterrupted()) {   // gestione dell'interruzione
                DatagramPacket packet = new DatagramPacket(new byte[size], size);
                // Ricevo il pacchetto.
                socket.receive(packet);
                System.out.println("Client: " +
                        new String(packet.getData(), packet.getOffset(),
                                packet.getLength()));
                System.out.print("> ");
            }
        }
         catch (Exception e) {
            System.err.println("Errore client: " + e.getMessage());
        }
    }

}
