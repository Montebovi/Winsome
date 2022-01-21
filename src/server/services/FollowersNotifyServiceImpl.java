package server.services;

import server.managers.FollowersManager;
import shared.dto.UserDto;
import shared.interfaces.IFollowersNotifyService;
import shared.interfaces.INotifyFollowerEvent;
import shared.utils.AppLogger;
import shared.utils.ServiceResultCodes;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

// classe che implementa IFollowersNotifyService
// Essa implementa la notifica dei follower sul client.
// Mette a disposizione metodi per avviare il servizio e gestire le registrazioni.
public class FollowersNotifyServiceImpl extends RemoteObject implements IFollowersNotifyService {
    // lista dei client registrati

    private final ArrayList<INotifyFollowerEvent> clients;  // elenco delle callback

    // contiene per ogni callback lo username a cui è correlata
    private final Map<INotifyFollowerEvent,String> clientsMap;

    private final FollowersManager followerManager;

    public FollowersNotifyServiceImpl(FollowersManager followerManager) throws RemoteException{
        super();
        clients = new ArrayList<INotifyFollowerEvent>();
        clientsMap = new HashMap<>();
        this.followerManager = followerManager;
    }

    // start del servizio.
    // registra per la RMI che viene invocata dal client per sottoscrivere la propria callback.
    public static IFollowersNotifyService StartService(String svcName, int port, FollowersManager followersManager ){
        try {
            FollowersNotifyServiceImpl server = new FollowersNotifyServiceImpl(followersManager);
            IFollowersNotifyService stub=(IFollowersNotifyService)
                    UnicastRemoteObject.exportObject (server,0);
            LocateRegistry.createRegistry(port);
            Registry registry=LocateRegistry.getRegistry(port);
            registry.bind (svcName, stub);
            AppLogger.log("Servizio follower notifier attivo sulla porta ["+port+"].");
            return server;
        } catch (Exception e) {
            AppLogger.log("Errore durante la FollowersNotifyServiceImpl.RegisterService: "+e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    // rigistrazione chiamata dal client per sottoscriversi.
    // NOTE: invia subito tramite callback l'elenco di tutti i followers.
    public synchronized void registerForCallback(String clientName, INotifyFollowerEvent clientInterface) throws RemoteException {
        if (!clients.contains(clientInterface)){
            clients.add(clientInterface);
            clientsMap.put(clientInterface,clientName);
            AppLogger.log(String.format("New client [%s] registered for Follower-events.",clientName));
            notifyAllUsers(clientInterface,clientName);  // primo allineamento: invia tutti i follower al client appena sottoscritto
        }
        else{
            AppLogger.log(String.format("Client already registered on the service for Follower-events [%s].",clientName));
        }
    }

    // deregistra la callback
    public synchronized void unregisterForCallback(String clientName, INotifyFollowerEvent clientInterface) throws RemoteException{
        if (clients.remove(clientInterface)) {
            AppLogger.log(String.format("Client [%s] unregistered for Follower-events.", clientName));
            clientsMap.remove(clientInterface);
        }
        else
            AppLogger.log(String.format("Unable to unregister for Follower-events [%s].",clientName));
    }

    // metodo per notificare aggiunta/rimozione di un follower
    public void notifyFollowerChanged(String clientUserName, UserDto follower, boolean isAdded) throws RemoteException {
        doCallback(clientUserName, follower, isAdded);
    }

    // notifica tutti i followers ad un utente
    private synchronized void notifyAllUsers(INotifyFollowerEvent clientInterface, String clientName) {
        var allFollowers = followerManager.getListFollowers(clientName);
        if (allFollowers.getResultCode() == ServiceResultCodes.SUCCESS) {
            var allUsers = allFollowers.getPayload().stream().map(u -> new UserDto(u.getUsername(), u.getTags())).toList();
            try {
                clientInterface.followerChangedEvent(allUsers, true);  // chiamata remota
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    // esegue cocretamente la chiamta callback sul client per notificare aggiunta/rimozione di un follower.
    private synchronized void doCallback(String clientUserName, UserDto follower, boolean isAdded) throws RemoteException {
        AppLogger.log("Call callback to "+clientUserName);
        Iterator i = clients.iterator( );
        while (i.hasNext()) {
            INotifyFollowerEvent cb = (INotifyFollowerEvent) i.next();
            if (clientsMap.get(cb).equals(clientUserName)) {
                var list = new ArrayList<UserDto>();
                list.add(follower);
                cb.followerChangedEvent(list, isAdded);  // callback sul client
            }
        }
    }
}
