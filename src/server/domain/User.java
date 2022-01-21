package server.domain;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

// rappresenta un utente
public class User {
    private int id;
    private String username;
    private String hashedpwd;   // hash della password
    private List<String> tags = new ArrayList<>();   // elenco di tags

    public User(){

    }

    public User(int id, String username, String hashedPassword, List<String> tags) throws NoSuchAlgorithmException {
        this.id=id;
        this.username=username;
        this.hashedpwd = hashedPassword;
        this.tags= tags;
    }

    public String getUsername(){
        return this.username;
    }

    public List<String> getTags(){
        return tags;
    }

    public boolean hasTag(String tag) {
        return tags.stream().anyMatch(tag::equalsIgnoreCase);
    }

    public String getHashedPwd(){
        return hashedpwd;
    }

    public int getId() {
        return id;
    }
}
