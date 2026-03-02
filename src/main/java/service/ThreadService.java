package service;

import entities.*;
import utils.MyDataBase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ThreadService implements ForumService<ForumThread> {

    private NotificationService notificationService = new NotificationService();
    private EmailService1 emailService1 = new EmailService1();
    private BadWordsFilterService badWordsFilterService = new BadWordsFilterService();
    private TranslationService translationService = new TranslationService(); // ✅ NEW

    @Override
    public void add(ForumThread thread) {
        badWordsFilterService.filterThread(thread);
        String sql = "INSERT INTO thread (titre, contenu, dateCreation, status, user_id, category, tags, views, likes, liked_by) VALUES (?,?,?,?,?,?,?,?,?,?)";
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pst.setString(1, thread.getTitre());
            pst.setString(2, thread.getContenu());
            pst.setTimestamp(3, Timestamp.valueOf(thread.getDateCreation()));
            pst.setString(4, thread.getStatus().name());
            pst.setInt(5, thread.getUser().getId());
            pst.setString(6, thread.getCategory());
            pst.setString(7, thread.getTags());
            pst.setInt(8, 0); // initial views
            pst.setInt(9, 0); // initial likes
            pst.setString(10, ""); // initial liked_by
            int rows = pst.executeUpdate();
            if (rows == 0) throw new SQLException("No rows affected.");
            try (ResultSet keys = pst.getGeneratedKeys()) {
                if (keys.next()) thread.setThreadId(keys.getInt(1));
            }
            System.out.println("✅ Thread ajouté ID=" + thread.getThreadId());

            // ✅ Send email to all Agriculteurs if thread author is an Expert
            try {
                UserService userService = new UserService();
                User author = userService.getUserById(thread.getUser().getId());
                if (author != null && author.getRole() == Role.EXPERT) {
                    System.out.println("📧 Expert thread detected - sending emails to Agriculteurs...");
                    emailService1.notifyAgriculteursOfNewExpertThread(thread, author);
                }
            } catch (Exception emailEx) {
                System.err.println("⚠️ Failed to send email notifications: " + emailEx.getMessage());
            }

        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override
    public void update(ForumThread thread) {
        update(thread, null);
    }

    public void update(ForumThread thread, User changedBy) {
        ThreadStatus oldStatus = null;
        String selectSql = "SELECT status FROM thread WHERE thread_id=?";
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement pst = conn.prepareStatement(selectSql)) {
            pst.setInt(1, thread.getThreadId());
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    oldStatus = ThreadStatus.valueOf(rs.getString("status"));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }

        String sql = "UPDATE thread SET titre=?, contenu=?, status=?, category=?, tags=? WHERE thread_id=?";
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, thread.getTitre());
            pst.setString(2, thread.getContenu());
            pst.setString(3, thread.getStatus().name());
            pst.setString(4, thread.getCategory());
            pst.setString(5, thread.getTags());
            pst.setInt(6, thread.getThreadId());
            pst.executeUpdate();
            System.out.println("✅ Thread modifié");

            if (changedBy != null && oldStatus != null && oldStatus != thread.getStatus()) {
                notificationService.notifyThreadStatusChange(thread, changedBy);
            }

        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override
    public void delete(int id) {
        String deleteResponseMedia =
                "DELETE FROM media WHERE response_id IN " +
                        "(SELECT response_id FROM response WHERE thread_id=?)";
        String deleteResponses     = "DELETE FROM response WHERE thread_id=?";
        String deleteThreadMedia   = "DELETE FROM media WHERE thread_id=?";
        String deleteNotifications = "DELETE FROM notification WHERE thread_id=?";
        String deleteThread        = "DELETE FROM thread WHERE thread_id=?";

        try (Connection conn = MyDataBase.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement p0 = conn.prepareStatement(deleteResponseMedia)) {
                    p0.setInt(1, id); p0.executeUpdate();
                }
                try (PreparedStatement p2 = conn.prepareStatement(deleteResponses)) {
                    p2.setInt(1, id); p2.executeUpdate();
                }
                try (PreparedStatement p3 = conn.prepareStatement(deleteThreadMedia)) {
                    p3.setInt(1, id); p3.executeUpdate();
                }
                try (PreparedStatement p4 = conn.prepareStatement(deleteNotifications)) {
                    p4.setInt(1, id); p4.executeUpdate();
                }
                try (PreparedStatement p5 = conn.prepareStatement(deleteThread)) {
                    p5.setInt(1, id); p5.executeUpdate();
                }
                conn.commit();
                System.out.println("✅ Thread supprimé ID=" + id);
            } catch (Exception e) {
                conn.rollback();
                System.err.println("❌ Rollback suppression thread #" + id);
                e.printStackTrace();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override
    public List<ForumThread> getAll() {
        List<ForumThread> list = new ArrayList<>();
        String sql = "SELECT * FROM thread";
        try (Connection conn = MyDataBase.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("user_id"));
                list.add(new ForumThread(
                        rs.getInt("thread_id"),
                        rs.getString("titre"),
                        rs.getString("contenu"),
                        rs.getTimestamp("dateCreation").toLocalDateTime(),
                        ThreadStatus.valueOf(rs.getString("status")),
                        user,
                        rs.getString("category"),
                        rs.getString("tags"),
                        rs.getInt("views"),
                        rs.getInt("likes"),
                        rs.getString("liked_by")
                ));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    public List<ForumThread> getAllThreads() { return getAll(); }

    public void incrementViews(int threadId) {
        String sql = "UPDATE thread SET views = views + 1 WHERE thread_id=?";
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, threadId);
            pst.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public boolean likeThread(int threadId, int userId) {
        try (Connection conn = MyDataBase.getConnection()) {
            String selectSql = "SELECT liked_by, likes FROM thread WHERE thread_id=?";
            String likedBy = "";
            int likes = 0;

            try (PreparedStatement pst = conn.prepareStatement(selectSql)) {
                pst.setInt(1, threadId);
                try (ResultSet rs = pst.executeQuery()) {
                    if (rs.next()) {
                        likedBy = rs.getString("liked_by");
                        if (likedBy == null) likedBy = "";
                        likes = rs.getInt("likes");
                    }
                }
            }

            String userIdStr = String.valueOf(userId);
            boolean alreadyLiked = false;

            if (!likedBy.isEmpty()) {
                String[] likedUsers = likedBy.split(",");
                for (String id : likedUsers) {
                    if (id.equals(userIdStr)) {
                        alreadyLiked = true;
                        break;
                    }
                }
            }

            String updateSql;
            if (alreadyLiked) {
                String[] likedUsers = likedBy.split(",");
                StringBuilder newLikedBy = new StringBuilder();
                for (String id : likedUsers) {
                    if (!id.equals(userIdStr) && !id.isEmpty()) {
                        if (newLikedBy.length() > 0) newLikedBy.append(",");
                        newLikedBy.append(id);
                    }
                }
                updateSql = "UPDATE thread SET likes = likes - 1, liked_by = ? WHERE thread_id=?";
                try (PreparedStatement pst = conn.prepareStatement(updateSql)) {
                    pst.setString(1, newLikedBy.toString());
                    pst.setInt(2, threadId);
                    pst.executeUpdate();
                }
                System.out.println("👎 Thread unliked");
                return false;
            } else {
                String newLikedBy = likedBy.isEmpty() ? userIdStr : likedBy + "," + userIdStr;
                updateSql = "UPDATE thread SET likes = likes + 1, liked_by = ? WHERE thread_id=?";
                try (PreparedStatement pst = conn.prepareStatement(updateSql)) {
                    pst.setString(1, newLikedBy);
                    pst.setInt(2, threadId);
                    pst.executeUpdate();
                }
                System.out.println("👍 Thread liked");

                try {
                    ForumThread thread = getById(threadId);
                    UserService userService = new UserService();
                    User likedByUser = userService.getUserById(userId);
                    if (thread != null && likedByUser != null) {
                        notificationService.notifyThreadLike(thread, likedByUser);
                    }
                } catch (Exception notifEx) {
                    System.err.println("⚠ Failed to create like notification: " + notifEx.getMessage());
                }

                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean hasUserLikedThread(int threadId, int userId) {
        String sql = "SELECT liked_by FROM thread WHERE thread_id=?";
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, threadId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    String likedBy = rs.getString("liked_by");
                    if (likedBy != null && !likedBy.isEmpty()) {
                        String[] likedUsers = likedBy.split(",");
                        String userIdStr = String.valueOf(userId);
                        for (String id : likedUsers) {
                            if (id.equals(userIdStr)) return true;
                        }
                    }
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    public ForumThread getById(int threadId) {
        String sql = "SELECT * FROM thread WHERE thread_id=?";
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, threadId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setId(rs.getInt("user_id"));
                    return new ForumThread(
                            rs.getInt("thread_id"),
                            rs.getString("titre"),
                            rs.getString("contenu"),
                            rs.getTimestamp("dateCreation").toLocalDateTime(),
                            ThreadStatus.valueOf(rs.getString("status")),
                            user,
                            rs.getString("category"),
                            rs.getString("tags"),
                            rs.getInt("views"),
                            rs.getInt("likes"),
                            rs.getString("liked_by")
                    );
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    // ==================== TRANSLATION METHODS ✅ NEW ====================

    /**
     * Get thread with translated content
     * @param threadId Thread ID
     * @param targetLanguage Target language code (e.g., "en", "ar", "fr")
     * @return Translated thread object
     */
    public ForumThread getByIdTranslated(int threadId, String targetLanguage) {
        ForumThread thread = getById(threadId);
        if (thread == null) {
            return null;
        }
        return translationService.translateThread(thread, targetLanguage);
    }

    /**
     * Get all threads with translation option
     * @param targetLanguage Target language (null to skip translation)
     * @return List of threads (translated if language specified)
     */
    public List<ForumThread> getAllTranslated(String targetLanguage) {
        List<ForumThread> threads = getAll();

        if (targetLanguage == null || targetLanguage.isEmpty()) {
            return threads;
        }

        List<ForumThread> translatedThreads = new ArrayList<>();
        for (ForumThread thread : threads) {
            try {
                ForumThread translated = translationService.translateThread(thread, targetLanguage);
                translatedThreads.add(translated);
            } catch (Exception e) {
                System.err.println("⚠️ Failed to translate thread " + thread.getThreadId());
                translatedThreads.add(thread); // Add original if translation fails
            }
        }

        return translatedThreads;
    }

    /**
     * Detect the language of a thread
     * @param threadId Thread ID
     * @return Language code (e.g., "fr", "en", "ar")
     */
    public String detectThreadLanguage(int threadId) {
        ForumThread thread = getById(threadId);
        if (thread == null) {
            return null;
        }
        return translationService.detectLanguage(thread.getTitre());
    }

    /**
     * Check if thread needs translation for user
     * @param threadId Thread ID
     * @param userLanguage User's preferred language
     * @return true if translation recommended
     */
    public boolean needsTranslation(int threadId, String userLanguage) {
        ForumThread thread = getById(threadId);
        if (thread == null) {
            return false;
        }
        return translationService.needsTranslation(thread.getTitre(), userLanguage);
    }
}