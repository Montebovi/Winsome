package server;

import shared.utils.AppLogger;

import java.io.IOException;

import static java.lang.Thread.sleep;

// main del server
// istanza WinsomeServer e lancia il servizio.
public class ServerMain {
    public static void main(String[] args) throws Exception {
        AppLogger.log("Winsome server is starting...");
        var server = new WinsomeServer();
        server.execute();
    }
}
