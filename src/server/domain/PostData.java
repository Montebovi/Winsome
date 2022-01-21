package server.domain;

import java.util.ArrayList;
import java.util.List;

// Classe che il Post con i dati aggiuntivi
public class PostData {
    Post post;   // post correlato
    int numPos;  // numero rate positivi
    int numNeg;  // numero rate negativi
    ArrayList<Comment> postComments;  // elenco dei commenti

    public PostData(Post post, int numPos, int numNeg, ArrayList<Comment> comments){
        this.post = post;
        this.numPos = numPos;
        this.numNeg = numNeg;
        this.postComments = new ArrayList<>();
        if (comments != null)
          postComments.addAll(comments);
    }

    public Post getPost() {
        return post;
    }

    public int getNumNeg() {
        return numNeg;
    }

    public int getNumPos() {
        return numPos;
    }

    public List<Comment> getPostComments() {
        return postComments;
    }
}
