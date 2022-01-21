package shared.interfaces;

import shared.dto.UserDto;

import java.rmi.*;
import java.util.List;

// interfaccia per la callback sul client che notifica aggiunta/rimozione di followers
public interface INotifyFollowerEvent extends Remote {
    // note: UserDto è serializable
    public void followerChangedEvent(List<UserDto> users, boolean added) throws RemoteException;
}
