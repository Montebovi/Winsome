package server.domain;

// Rate di un post.
public class PostRate {
    int idPost;   // id del post a cui si riferisce il rate
    int idVotant; // id dell'utente che ha espresso il voto
    int vote;     // il valore del voto; vale +1 o -1
    public boolean isCounted; // true solo se è già sato contato in una delle iterazioni precenti

    public PostRate(int idPost, int idVotant, int vote){
        this.idPost = idPost;
        this.idVotant = idVotant;
        this.vote = vote;
        this.isCounted = false;
    }


    public int getVote() {
        return vote;
    }

    public int getIdPost() {
        return idPost;
    }

    public int getIdVotant() {
        return idVotant;
    }

}