package server.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import server.domain.User;
import server.utils.Hasher;
import shared.utils.ServiceResultCodes;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

// classe per la gestione degli utenti
// Lo stato consiste nella lista degli utenti. Esso viene memorizzato su json per ripristinare il server ad un riavvio.
public class UserManager {
    static final int  MAX_NUM_TAGS = 5;
    static final String UserListFileName = "UserList.json";

    private List<User> usersList;

    // inizializzazione che consiste nella creazione della lista degli utenti, o ripristino da json.
    public void initialize() throws IOException {
        var f = new File(UserListFileName);

        if(f.exists() && !f.isDirectory()) {
            var inputStream = new FileInputStream(UserListFileName);
            var reader = new JsonReader(new InputStreamReader(inputStream));
            var collectionType = new TypeToken<ArrayList<User>>(){}.getType();
            usersList = new Gson().fromJson(reader,collectionType) ;
            inputStream.close();
        }
        else
            usersList = new ArrayList<User>();
    }

    // register di un utente.
    // Si effettuano diversi controlli di validazione.
    public synchronized int register(String username, String password, List<String> tags) throws Exception {
        if(username.length()==0)
            return ServiceResultCodes.ERR_INVALID_USERNAME;

        if(getUser(username)!=null)
            return ServiceResultCodes.ERR_USERNAME_USED;
        if(password.length()<=4)
            return ServiceResultCodes.ERR_PASSWORD_TOOSHORT;
        if(tags.size()>MAX_NUM_TAGS)
            return ServiceResultCodes.ERR_TOO_MUCH_TAGS;

        createUser(username, password, tags);
        return ServiceResultCodes.SUCCESS;
    }

    // login dell'utente.
    public synchronized int login(String username, String pwd) throws Exception {
        var user = getUser(username);
        if (user==null)
            return ServiceResultCodes.ERR_USERNAME_NOT_EXISTING;
        Hasher hasher = new Hasher();
        if (!hasher.isValidHashPassword(pwd,user.getHashedPwd()) /*|| user.login==true*/)
            return ServiceResultCodes.ERR_WRONG_PWD;
        return ServiceResultCodes.SUCCESS;
    }

    // logout utente.
    public synchronized int logout(String username){
        var user = getUser(username);
        if(user==null)
            return ServiceResultCodes.ERR_USERNAME_NOT_EXISTING;
        return ServiceResultCodes.SUCCESS;
    }

    // ritorna l'utente con lo username specificato.
    public synchronized User getUser (String username){
        return usersList.stream().filter(user -> user.getUsername().equals(username)).findAny().orElse(null);
    }

    // ritorna lista di utente che hanno almeno un tag in comune con l'utente 'username'.
    public synchronized List<User> listUsers(String username) {
        var user = getUser(username);
        if (user == null)
            return new ArrayList<User>();
        List<User> similarUsers = new ArrayList<User>();
        for(User u:usersList){
            if(!u.getUsername().equals(user.getUsername())){
                if(existsCommonTag(u, user))
                    similarUsers.add(u);
            }
        }
        return similarUsers;
    }

    // ritorna utente attraverso il suo id
    public synchronized  User getUserById(int idUser) {
        return usersList.stream().filter(user -> user.getId()==idUser).findAny().orElse(null);
    }

    // crea un nuovo utente
    private User createUser(String username, String password, List<String> tags) throws Exception {
        Hasher hasher = new Hasher();
        var hashedPwd = hasher.Hash(password);
        var idUser = getNewId();
        User newUser = new User(idUser, username, hashedPwd, tags);
        usersList.add(newUser);
        saveUserList(usersList);
        return newUser;
    }

    // ritorna un nuovo id da assegnare ad un nuovo utente.
    private int getNewId() {
        var maxId = usersList.stream().mapToInt(x -> x.getId()).max().orElse(0);
        return maxId+1;
    }

    // Ritorna true se i due utenti hanno almeno un tag in comune.
    private boolean existsCommonTag(User u1, User u2) {
        for(String t:u1.getTags()){
            if(u2.hasTag(t))
                return true;
        }
        return false;
    }

    // salvataggio della lista utenti su file json.
    private void saveUserList(List<User> usersList) throws IOException {
        var gson = new GsonBuilder().setPrettyPrinting().create();
        FileOutputStream fos = new FileOutputStream(UserListFileName);
        OutputStreamWriter ow = new OutputStreamWriter(fos);
        String usersJson = gson.toJson(usersList);
        ow.write(usersJson);
        ow.flush();
        ow.close();
    }
}
