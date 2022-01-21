package shared.dto;

import java.util.ArrayList;
import java.util.Collection;

// Data Transfer Object per il dettaglio del post
// Deriva da PostDto in quanto rappresenta un arricchimento.
public class PostDetailDto extends PostDto {

    public int numPos;  // numero positivi
    public int numNeg;  // numero negativi
    public String contenuto;  // contenuto del messaggio
    private ArrayList<CommentDto> comments;  // elenco dei commanti

    public PostDetailDto(int idPost, String titolo, String autore, String contenuto) {
        super(idPost, titolo, autore);
        this.contenuto = contenuto;
        comments = new ArrayList<>();
    }

    public void setNumNeg(int numNeg) {
        this.numNeg = numNeg;
    }

    public void setNumPos(int numPos) {
        this.numPos = numPos;
    }

    public void addComment(String authorName, String textOfComment) {
        comments.add(new CommentDto(authorName, textOfComment));
    }

    public Collection<CommentDto> getComments() {
        return comments;
    }

    public String getContenuto() {
        return contenuto;
    }
}


