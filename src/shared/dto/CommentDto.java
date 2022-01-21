package shared.dto;

// Data Transfer Object per il comando
public class CommentDto {
    public String author;
    public String comment;

    public CommentDto(String author, String comment){
        this.author = author;
        this.comment = comment;
    }
}
