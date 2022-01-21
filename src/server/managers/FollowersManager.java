package server.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import server.domain.Result;
import server.domain.User;
import shared.Response;
import shared.utils.AppLogger;
import shared.utils.ServiceResultCodes;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

// Classe per gestire i follower.
// Per maggiore efficienza mantiene due map:
//                  (1) id utente -> lista id dei followers
//                  (2) id utente -> lista id dei following (utenti seguiti)
// Le due mappe vengono memorizzate su distinti file json per ripristinare lo stato del server in situazione di riavvio.
// Per sicurezza i file json vengono salvati ad ogni modifica delle due map.
public class FollowersManager {
    private UserManager userMan;
    private HashMap<Integer, HashSet<Integer>> followersList; // elenco delle persone che seguono l'utente x
    private HashMap<Integer, HashSet<Integer>> followingList; // elenco delle persone seguite dall'utente x

    private static final String FileNameListFollowers = "UsersFollowers.json";  // nome del file per il json
    private static final String FileNameListFollowing = "UsersFollowing.json";  // nome del file per il json

    public FollowersManager(UserManager userMan){
        this.userMan= userMan;
    }

    // Inizializzazione: creazione delle due map con recupero da json (se presenti)
    public void initialize() throws FileNotFoundException {
        followersList = new HashMap<>();
        followingList = new HashMap<>();

        File followersFile = new File(FileNameListFollowers);
        if (followersFile.exists() && !followersFile.isDirectory()) {
            FileInputStream inputStream = new FileInputStream(FileNameListFollowers);
            JsonReader reader = new JsonReader(new InputStreamReader(inputStream));
            Type collectionType = new TypeToken<HashMap<Integer, HashSet<Integer>>>() {
            }.getType();
            followersList = new Gson().fromJson(reader, collectionType);
        }

        File followingFile = new File(FileNameListFollowing);
        if (followingFile.exists() && !followingFile.isDirectory()) {
            FileInputStream inputStream = new FileInputStream(FileNameListFollowing);
            JsonReader reader = new JsonReader(new InputStreamReader(inputStream));
            Type collectionType = new TypeToken<HashMap<Integer, HashSet<Integer>>>() {
            }.getType();
            followingList = new Gson().fromJson(reader, collectionType);
        }
    }

    // ritorna elenco degli utenti seguiti dall'utente user come List
    public synchronized ArrayList<User> getListFollowing(User user){
        var following = new ArrayList<User>();

        if(followingList.get(user.getId())==null)
            return following;

        for(int idUser:followingList.get(user.getId()))
            following.add(userMan.getUserById(idUser));

        return following;
    }

    // restituisce elenco degli id di utenti dei seguiti da user
    public synchronized HashSet<Integer> getListFollowingId(User user){
        var following =followingList.get(user.getId());
        if (following == null)
            return new HashSet<Integer>();
        return following;
    }

    // ritorna elenco degli utenti che segono  l'utente user (followers)
    public synchronized Result<List<User>> getListFollowers(String username){
        var user = userMan.getUser(username);
        if (user == null)
            return new Result<>(null,ServiceResultCodes.ERR_INVALID_USERNAME);
        var res = new Result<List<User>>(getListFollowers(user),ServiceResultCodes.SUCCESS);
        return res;
    }

    // aggiunge un follower
    //      user: utente che intende essere follower di username
    //      username: utente che viene seguito da user
    // note: vengono aggiornate entrambe le map.
    public synchronized int followUser(User user, String username) {
        if(user.getUsername().equals(username))
            return ServiceResultCodes.ERR_INVALID_USERNAME;
        if (userMan.getUser(username) == null)
            return ServiceResultCodes.ERR_USERNAME_NOT_EXISTING;

        var userFollowed = userMan.getUser(username);

        var result = addFollowing(user, userFollowed);
        if(result!=ServiceResultCodes.SUCCESS)
            return result;

        result = addFollower(userFollowed,user);
        if(result!=ServiceResultCodes.SUCCESS)
            return result;

        saveData();
        return ServiceResultCodes.SUCCESS;
    }

    // rimuove un follower
    //      user: utente che intende non essere più follower di username
    //      username: utente che non viene più seguito da user
    // note: vengono aggiornate entrambe le map.
    public synchronized int unfollowUser(User user, String username)  {
        if(user.getUsername().equals(username))
            return ServiceResultCodes.ERR_INVALID_USERNAME;
        if (userMan.getUser(username) == null)
            return ServiceResultCodes.ERR_USERNAME_NOT_EXISTING;

        var userFollowed = userMan.getUser(username);

        var result = removeFollowing(user, userFollowed);
        if(result!=ServiceResultCodes.SUCCESS)
            return result;

        result = removeFollower(userFollowed,user);
        if(result!=ServiceResultCodes.SUCCESS)
            return result;

        saveData();
        return ServiceResultCodes.SUCCESS;
    }

    // restituisce la lista dei follower di user
    private ArrayList<User> getListFollowers(User user){
        var followers =  new ArrayList<User>();
        if(followersList.get(user.getId())== null)
            return followers;

        for(int idUser:followersList.get(user.getId())){
            followers.add(userMan.getUserById(idUser));
        }

        return followers;
    }

    // implementazione di add follower
    private int addFollower(User user, User userFollower) {
        var idFollower = userFollower.getId();

        if (followersList.get(user.getId()) == null) {
            var followerSet = new HashSet<Integer>();
            followerSet.add(idFollower);
            followersList.put(user.getId(), followerSet);
        }
        else {
            var followerUsers = followersList.get(user.getId());
            if(!followerUsers.contains(idFollower)){
                followerUsers.add(idFollower);
            }
            else
                return ServiceResultCodes.ERR_ALREADY_FOLLOWERS;
        }
        return ServiceResultCodes.SUCCESS;
    }

    // implementazione di remove follower
    private int removeFollower(User user, User userFollower){
        var idFollower = userFollower.getId();
        if (followersList.get(user.getId()) == null)
            return ServiceResultCodes.ERR_USER_NOT_FOLLOWED;

        var followerUsers = followersList.get(user.getId());

        boolean removed = followerUsers.remove(userFollower.getId());
        return removed ? ServiceResultCodes.SUCCESS : ServiceResultCodes.ERR_USER_NOT_FOLLOWED;
    }

    // implementazione add following
    private int addFollowing(User user, User userFollowed)  {
        var idFollowed = userFollowed.getId();
        if (followingList.get(user.getId()) == null) {
            var followedSet = new HashSet<Integer>();
            followedSet.add(idFollowed);
            followingList.put(user.getId(), followedSet);
        }
        else{
            var followedUsers = followingList.get(user.getId());
            if(!followedUsers.contains(idFollowed)){
                followedUsers.add(idFollowed);
            }
            else
                return ServiceResultCodes.ERR_ALREADY_FOLLOWERS;
        }
        return ServiceResultCodes.SUCCESS;
    }

    // implementazione remove following
    private int removeFollowing(User user, User userFollowed) {
        var idFollowed = userFollowed.getId();
        if (followingList.get(user.getId()) == null)
            return ServiceResultCodes.ERR_USER_NOT_FOLLOWED;
        var followedUsers = followingList.get(user.getId());

        boolean removed = followedUsers.remove(idFollowed);
        return removed ? ServiceResultCodes.SUCCESS : ServiceResultCodes.ERR_USER_NOT_FOLLOWED;
    }

    // salvataggio dello stato (rappresentato dalle due map) su file json
    private void saveData() {
        String json;
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        try {
            var fosFollowers = new FileOutputStream(FileNameListFollowers);
            var owFollowers = new OutputStreamWriter(fosFollowers);
            json = gson.toJson(followersList);
            owFollowers.write(json);
            owFollowers.flush();
        } catch (IOException e) {
            e.printStackTrace();
            AppLogger.log("Errore salvataggio file json: " + e.getMessage());
        }

        try {
            var fosFollowing = new FileOutputStream(FileNameListFollowing);
            var owFollowing = new OutputStreamWriter(fosFollowing);
            json = gson.toJson(followingList);
            owFollowing.write(json);
            owFollowing.flush();
        } catch (IOException e) {
            e.printStackTrace();
            AppLogger.log("Errore salvataggio file json: " + e.getMessage());
        }
    }
}
