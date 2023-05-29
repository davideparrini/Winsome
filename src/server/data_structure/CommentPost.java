package server.data_structure;

public class CommentPost {
    private String author;
    private String content;


    public CommentPost() {
    }

    public CommentPost(String author, String content) {
        this.author = author;
        this.content = content;
    }

    @Override
    public String toString() {
        return author + ": \"" + content + "\"";
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
