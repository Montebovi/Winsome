package client;

import shared.dto.UserDto;
import shared.interfaces.IFollowersNotifyService;
import shared.interfaces.INotifyFollowerEvent;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

// classe che tiene memorizzato elenco dei follower in locale
public class FollowersCache {

    private final String svcName;
    private final int port;
    private final String svcHost;
    private IFollowersNotifyService server;
    private INotifyFollowerEvent stubCbObj;
    private String username;
    private ArrayList<UserDto> followerUsers;

    public FollowersCache(String svcHost, String svcName, int port) {
        this.followerUsers = new ArrayList<>();
        this.svcHost = svcHost;
        this.svcName = svcName;
        this.port = port;
    }

    public void initialize() throws RemoteException, NotBoundException {
        Registry registry = LocateRegistry.getRegistry(svcHost, port);
        server = (IFollowersNotifyService) registry.lookup(svcName);
    }

    // registrazione della callback
    public void registerForCallback(String username) {
        if (stubCbObj != null) {
            System.out.println("Registrazione già effettuata.");
            return;
        }

        try {
            System.out.print("Registering for callback...");
            var callbackObj = new NotifyFollowerEventImpl(this);
            var stub = (INotifyFollowerEvent) UnicastRemoteObject.exportObject(callbackObj, 0);
            server.registerForCallback(username, stub);  //nota: si passa anche lo username
            System.out.println("DONE");
            stubCbObj = stub;
            this.username = username;
        } catch (Exception e) {
            System.out.println("ERRORE in NotifyFollowerEventImpl.registerForCallback: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // deregistrazione della callback sul server
    public void unregisterForCallback() {
        try {
            followerUsers.clear();  // la lista dei followers viene azzerata
            if (server != null && stubCbObj != null) {
                System.out.print("UnrRegistering for callback...");
                server.unregisterForCallback(username, stubCbObj);
                stubCbObj = null;
                System.out.println("DONE");
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    // metodo interno per aggiornare la cache.
    // parametro added: true => nuovo utente follower; false => utente non più follower
    // NOTA: si rimuove in ogni caso per poi aggiungere; in questo modo si aggiorna eventuale follower già esistente.
    void UpdateCache(List<UserDto> users, boolean added){
        for(var u:users){
            // rimuove in ogni caso perchè potrebbe avere tags modificati
            followerUsers.removeIf(x -> x.username.equals(u.username));
            if (added)
              this.followerUsers.add(u);
        }
    }

    // restituisce la lista locale (invocata al comando fi get-followers
    public List<UserDto> getFollowersForCurrentUser() {
        return this.followerUsers;
    }
}
