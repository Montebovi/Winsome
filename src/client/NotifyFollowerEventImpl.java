package client;

import shared.dto.UserDto;
import shared.interfaces.IFollowersNotifyService;
import shared.interfaces.INotifyFollowerEvent;

import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;
import java.util.List;

// implementazione della callback RMI invocata dal server
public class NotifyFollowerEventImpl extends RemoteObject implements INotifyFollowerEvent {

    private final FollowersCache cache;

    public NotifyFollowerEventImpl(FollowersCache cache) {
        super();
        this.cache = cache;
    }

    // aggiorna la cache dei followers.
    // Produce feedback a video.
    @Override
    public void followerChangedEvent(List<UserDto> followerUsers, boolean added) throws RemoteException {
        cache.UpdateCache(followerUsers,added);

        for(var u:followerUsers) {
            var tags = String.join(";",u.getTags());
            System.out.println(String.format("ricevuto followerChangedEvent([%s],[%s] tags:[%s])", u.username, added ? "aggiunto" : "rimosso",tags));
        }
        if (followerUsers.size() > 0)
          System.out.print("> ");
    }
}
