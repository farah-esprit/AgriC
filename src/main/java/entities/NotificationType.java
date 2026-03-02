package entities;

public enum NotificationType {
    NEW_RESPONSE,           // Someone replied to a thread
    THREAD_STATUS_CHANGE,   // Thread status changed (OPEN, RESOLVED, CLOSED)
    MENTION,                // User was mentioned in a response
    THREAD_REPLY            // New reply to user's thread
}
