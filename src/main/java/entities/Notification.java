package entities;

import java.time.LocalDateTime;

public class Notification {
    
    private int notificationId;
    private User user;                  // FK user_id (recipient)
    private NotificationType type;
    private String message;
    private ForumThread thread;         // FK thread_id (optional)
    private Response response;          // FK response_id (optional)
    private boolean isRead;
    private LocalDateTime dateCreation;
    
    // Constructors
    public Notification() {
    }
    
    public Notification(int notificationId, User user, NotificationType type, 
                       String message, ForumThread thread, Response response, 
                       boolean isRead, LocalDateTime dateCreation) {
        this.notificationId = notificationId;
        this.user = user;
        this.type = type;
        this.message = message;
        this.thread = thread;
        this.response = response;
        this.isRead = isRead;
        this.dateCreation = dateCreation;
    }
    
    public Notification(User user, NotificationType type, String message, 
                       ForumThread thread, Response response) {
        this.user = user;
        this.type = type;
        this.message = message;
        this.thread = thread;
        this.response = response;
        this.isRead = false;
        this.dateCreation = LocalDateTime.now();
    }
    
    // Getters and Setters
    public int getNotificationId() {
        return notificationId;
    }
    
    public void setNotificationId(int notificationId) {
        this.notificationId = notificationId;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public NotificationType getType() {
        return type;
    }
    
    public void setType(NotificationType type) {
        this.type = type;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public ForumThread getThread() {
        return thread;
    }
    
    public void setThread(ForumThread thread) {
        this.thread = thread;
    }
    
    public Response getResponse() {
        return response;
    }
    
    public void setResponse(Response response) {
        this.response = response;
    }
    
    public boolean isRead() {
        return isRead;
    }
    
    public void setRead(boolean read) {
        isRead = read;
    }
    
    public LocalDateTime getDateCreation() {
        return dateCreation;
    }
    
    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }
    
    @Override
    public String toString() {
        return "Notification{" +
                "notificationId=" + notificationId +
                ", type=" + type +
                ", message='" + message + '\'' +
                ", isRead=" + isRead +
                ", dateCreation=" + dateCreation +
                '}';
    }
}
