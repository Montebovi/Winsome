package shared.interfaces;

import java.rmi.Remote;
import java.util.List;

// interfaccia per invocare la regsiter sul server attraverso RMI
public interface IRegisterService extends Remote {
    int register(String username, String password, List<String> tags ) throws Exception;
}