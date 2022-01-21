package server.domain;

import java.util.ArrayList;
import java.util.Date;

// classe che rappresenta un commento di un post
public class Comment {
    String idCommento;
    int idPost;  // id al post correlato
    int idAuthor;  // id dell'autore del commento
    public boolean isCounted; // indicatore usato internamente per marcare se è stato considerato per il calcolo delle ricompense
    String text;  // testo del commento


    public Comment(String idCommento, int idPost, int idAuthor, String text){
        this.idCommento = idCommento;
        this.idPost = idPost;
        this.idAuthor = idAuthor;
        this.text = text;
        this.isCounted = false;
    }

    public int getIdAuthor() {
        return idAuthor;
    }

    public String getText() {
        return text;
    }


    public int getIdPost() {
        return idPost;
    }
}


