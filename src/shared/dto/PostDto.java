package shared.dto;

// Data Transfer Object per il post (solo dati utili per gli elenchi)
public class PostDto {
    public int idPost;
    public String titolo;
    public String autore ;

    public PostDto(int idPost, String titolo, String autore){

        this.idPost = idPost;
        this.titolo = titolo;
        this.autore = autore;
    }
}


