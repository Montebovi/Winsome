package shared.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

// Data Transfer Object per l'utente
// NOTE: Implementa serializabile perchè viene usato in RMI callback per notifica followers
public class UserDto implements Serializable {
    public String username;
    private ArrayList<String> tags = new ArrayList<String>();

    public UserDto(String username, List<String> tags) {
        this.username = username;
        if (tags != null)
            this.tags = new ArrayList<>(tags);
    }

    public List<String> getTags(){
        return tags;
    }
}
