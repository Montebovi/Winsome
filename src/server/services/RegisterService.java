package server.services;

import server.managers.UserManager;
import shared.interfaces.IRegisterService;

import java.util.List;

// classe che implementa l'interfaccia RMI per il comando "REGISTER"
public class RegisterService implements IRegisterService {
    private final UserManager userman;

    public RegisterService(UserManager userman){
        this.userman= userman;
    }

    // effettua registrazione utilizzando istanza di UserManager
    @Override
    public int register(String username, String password, List<String> tags) throws Exception {
        return userman.register(username,password,tags);
    }
}