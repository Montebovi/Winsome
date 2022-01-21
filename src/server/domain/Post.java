package server.domain;

import java.util.ArrayList;

// classe che rappresenta un Post.
//
public class Post {
    private int idPost;
    private int idPublisher; //utente che ha creato/rewin del post
    private int idAuthor; // non coindice con il publisher quando è un rewin perch+ rappresenta sempre l'autore del post originale
    private Integer idOriginalPost = null; //id del post originale (diverso da idPost quando è un rewin)
    private String titolo; //null se idOriginalPost!=null
    private String contenuto;
    private int eta;   // eta che occorre per il calcolo guadagno sul wallet

    public Post(int idPost, String titolo, String contenuto,int idAuthor, int idPublisher){
        this.idPost = idPost;
        this.titolo = titolo;
        this.contenuto = contenuto;
        this.idAuthor = idAuthor;
        this.idPublisher = idPublisher;
        eta = 0;
    }

    public int incEta() {
        ++eta;
        return eta;
    }

    public int getIdAuthor() {
        return idAuthor;
    }

    public int getIdPublisher() {
        return idPublisher;
    }

    public int getIdPost() {
        return idPost;
    }

    public Integer getIdOriginalPost() {
        return idOriginalPost;
    }

    public String getTitolo() {
        return titolo;
    }

    public String getContenuto() {
        return contenuto;
    }

    public void setIdOriginalPost(Integer idOriginalPost) {
        this.idOriginalPost = idOriginalPost;
    }

}

