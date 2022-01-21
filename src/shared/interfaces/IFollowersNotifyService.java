package shared.interfaces;

import shared.dto.UserDto;
import java.rmi.Remote;
import java.rmi.RemoteException;

// interfaccia per registrare/deregistrare la callback del client
public interface IFollowersNotifyService extends Remote {

    // registrazione callback del client
    public void registerForCallback(String clientname, INotifyFollowerEvent clientInterface) throws RemoteException;

    // deregistrazione callback del client
    public void unregisterForCallback(String clientname, INotifyFollowerEvent clientInterface) throws RemoteException;

    // metodo per notificare variazione follower (chiamata dai task), la quale implementazione effettua la callback sul client
    public void notifyFollowerChanged(String clientUserName, UserDto followerUser, boolean isAdded)
            throws RemoteException;
}
