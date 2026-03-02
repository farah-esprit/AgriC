package service;

import entities.*;
import utils.MyDataBase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NotificationService {

    /**
     * Create a new notification
     */
    public void add(Notification notification) {
        String sql = "INSERT INTO notification (user_id, type, message, thread_id, response_id, is_read, dateCreation) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pst.setInt(1, notification.getUser().getId());
            pst.setString(2, notification.getType().name());
            pst.setString(3, notification.getMessage());

            // Thread ID (nullable)
            if (notification.getThread() != null) {
                pst.setInt(4, notification.getThread().getThreadId());
            } else {
                pst.setNull(4, Types.INTEGER);
            }

            // Response ID (nullable)
            if (notification.getResponse() != null) {
                pst.setInt(5, notification.getResponse().getResponseId());
            } else {
                pst.setNull(5, Types.INTEGER);
            }

            pst.setBoolean(6, notification.isRead());
            pst.setTimestamp(7, Timestamp.valueOf(notification.getDateCreation()));

            pst.executeUpdate();

            ResultSet rs = pst.getGeneratedKeys();
            if (rs.next()) {
                notification.setNotificationId(rs.getInt(1));
            }

            System.out.println("✅ Notification créée ID=" + notification.getNotificationId());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get all notifications for a specific user
     */
    public List<Notification> getByUser(int userId) {
        List<Notification> list = new ArrayList<>();
        String sql = "SELECT * FROM notification WHERE user_id=? ORDER BY dateCreation DESC";

        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setInt(1, userId);

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    list.add(extractNotificationFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    /**
     * Get unread notifications for a user
     */
    public List<Notification> getUnreadByUser(int userId) {
        List<Notification> list = new ArrayList<>();
        String sql = "SELECT * FROM notification WHERE user_id=? AND is_read=FALSE ORDER BY dateCreation DESC";

        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setInt(1, userId);

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    list.add(extractNotificationFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    /**
     * Count unread notifications for a user
     */
    public int countUnreadByUser(int userId) {
        String sql = "SELECT COUNT(*) FROM notification WHERE user_id=? AND is_read=FALSE";

        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setInt(1, userId);

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }

    /**
     * Mark a notification as read
     */
    public void markAsRead(int notificationId) {
        String sql = "UPDATE notification SET is_read=TRUE WHERE notification_id=?";

        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setInt(1, notificationId);
            pst.executeUpdate();

            System.out.println("✅ Notification marquée comme lue ID=" + notificationId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Mark all notifications as read for a user
     */
    public void markAllAsReadByUser(int userId) {
        String sql = "UPDATE notification SET is_read=TRUE WHERE user_id=? AND is_read=FALSE";

        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setInt(1, userId);
            int rows = pst.executeUpdate();

            System.out.println("✅ " + rows + " notifications marquées comme lues");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Delete a notification
     */
    public void delete(int notificationId) {
        String sql = "DELETE FROM notification WHERE notification_id=?";

        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setInt(1, notificationId);
            pst.executeUpdate();

            System.out.println("✅ Notification supprimée ID=" + notificationId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Delete all read notifications for a user
     */
    public void deleteReadByUser(int userId) {
        String sql = "DELETE FROM notification WHERE user_id=? AND is_read=TRUE";

        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setInt(1, userId);
            int rows = pst.executeUpdate();

            System.out.println("✅ " + rows + " notifications supprimées");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Create notification when someone replies to a thread
     */
    public void notifyThreadOwner(ForumThread thread, Response response) {
        // Don't notify if the thread owner replied to their own thread
        if (thread.getUser().getId() == response.getUser().getId()) {
            return;
        }

        String message = response.getUser().getNom() + " a répondu à votre discussion: " + thread.getTitre();

        Notification notification = new Notification(
                thread.getUser(),
                NotificationType.THREAD_REPLY,
                message,
                thread,
                response
        );

        add(notification);
    }

    /**
     * Create notification when thread status changes
     */
    public void notifyThreadStatusChange(ForumThread thread, User changedBy) {
        // Don't notify if the owner changed their own thread status
        if (thread.getUser().getId() == changedBy.getId()) {
            return;
        }

        String message = "Le statut de votre discussion '" + thread.getTitre() +
                "' a été changé à: " + thread.getStatus().name();

        Notification notification = new Notification(
                thread.getUser(),
                NotificationType.THREAD_STATUS_CHANGE,
                message,
                thread,
                null
        );

        add(notification);
    }

    /**
     * ✅ NEW: Create notification when someone likes a thread
     */
    public void notifyThreadLike(ForumThread thread, User likedBy) {
        // Don't notify if user liked their own thread
        if (thread.getUser().getId() == likedBy.getId()) {
            return;
        }

        String message = likedBy.getNom() + " a aimé votre discussion: " + thread.getTitre();

        Notification notification = new Notification(
                thread.getUser(),
                NotificationType.THREAD_REPLY, // Using THREAD_REPLY type for likes (could add THREAD_LIKE if enum is extended)
                message,
                thread,
                null
        );

        add(notification);
    }

    /**
     * ✅ NEW: Create notification when someone likes a response
     */
    public void notifyResponseLike(Response response, User likedBy, ForumThread thread) {
        // Don't notify if user liked their own response
        if (response.getUser().getId() == likedBy.getId()) {
            return;
        }

        String message = likedBy.getNom() + " a aimé votre commentaire dans: " + thread.getTitre();

        Notification notification = new Notification(
                response.getUser(),
                NotificationType.THREAD_REPLY, // Using THREAD_REPLY type for likes (could add RESPONSE_LIKE if enum is extended)
                message,
                thread,
                response
        );

        add(notification);
    }

    /**
     * Helper method to extract notification from ResultSet
     */
    private Notification extractNotificationFromResultSet(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("user_id"));

        ForumThread thread = null;
        int threadId = rs.getInt("thread_id");
        if (!rs.wasNull()) {
            thread = new ForumThread();
            thread.setThreadId(threadId);
        }

        Response response = null;
        int responseId = rs.getInt("response_id");
        if (!rs.wasNull()) {
            response = new Response();
            response.setResponseId(responseId);
        }

        return new Notification(
                rs.getInt("notification_id"),
                user,
                NotificationType.valueOf(rs.getString("type")),
                rs.getString("message"),
                thread,
                response,
                rs.getBoolean("is_read"),
                rs.getTimestamp("dateCreation").toLocalDateTime()
        );
    }
}