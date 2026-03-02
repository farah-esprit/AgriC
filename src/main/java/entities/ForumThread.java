package entities;

import java.time.LocalDateTime;

public class ForumThread {

    private int threadId;
    private String titre;
    private String contenu;
    private LocalDateTime dateCreation;
    private ThreadStatus status;
    private User user;

    // NEW FIELDS
    private String category;
    private String tags;
    private int views;
    private int likes;
    private String likedBy;  // comma-separated user IDs

    public ForumThread() {
    }

    public ForumThread(int threadId, String titre, String contenu,
                       LocalDateTime dateCreation, ThreadStatus status,
                       User user, String category, String tags, int views, int likes, String likedBy) {

        this.threadId = threadId;
        this.titre = titre;
        this.contenu = contenu;
        this.dateCreation = dateCreation;
        this.status = status;
        this.user = user;
        this.category = category;
        this.tags = tags;
        this.views = views;
        this.likes = likes;
        this.likedBy = likedBy;
    }

    public ForumThread(String titre, String contenu,
                       LocalDateTime dateCreation, ThreadStatus status,
                       User user, String category, String tags) {

        this.titre = titre;
        this.contenu = contenu;
        this.dateCreation = dateCreation;
        this.status = status;
        this.user = user;
        this.category = category;
        this.tags = tags;
    }

    public int getThreadId() {
        return threadId;
    }

    public void setThreadId(int threadId) {
        this.threadId = threadId;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public ThreadStatus getStatus() {
        return status;
    }

    public void setStatus(ThreadStatus status) {
        this.status = status;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public int getViews() {
        return views;
    }

    public void setViews(int views) {
        this.views = views;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public String getLikedBy() {
        return likedBy;
    }

    public void setLikedBy(String likedBy) {
        this.likedBy = likedBy;
    }
}