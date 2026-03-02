package entities;


import java.time.LocalDateTime;

public class Response {

    private int responseId;
    private String contenu;
    private LocalDateTime dateCreation;
    private User user;              // FK user_id
    private ForumThread thread;     // FK thread_id
    private int likes;
    private String likedBy;         // comma-separated user IDs

    public Response() {
    }

    public Response(int responseId, String contenu,
                    LocalDateTime dateCreation, User user, ForumThread thread, int likes, String likedBy) {
        this.responseId = responseId;
        this.contenu = contenu;
        this.dateCreation = dateCreation;
        this.user = user;
        this.thread = thread;
        this.likes = likes;
        this.likedBy = likedBy;
    }

    public Response(String contenu,
                    LocalDateTime dateCreation, User user, ForumThread thread) {
        this.contenu = contenu;
        this.dateCreation = dateCreation;
        this.user = user;
        this.thread = thread;
    }

    // Getters & Setters

    public int getResponseId() { return responseId; }
    public void setResponseId(int responseId) { this.responseId = responseId; }

    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public ForumThread getThread() { return thread; }
    public void setThread(ForumThread thread) { this.thread = thread; }

    public int getLikes() { return likes; }
    public void setLikes(int likes) { this.likes = likes; }

    public String getLikedBy() { return likedBy; }
    public void setLikedBy(String likedBy) { this.likedBy = likedBy; }
}