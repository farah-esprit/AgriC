package service;

import entities.*;
import utils.MyDataBase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ResponseService implements ForumService<Response> {

    private NotificationService notificationService = new NotificationService();
    private ThreadService threadService = new ThreadService();
    private BadWordsFilterService badWordsFilterService = new BadWordsFilterService();

    @Override
    public void add(Response response) {
        badWordsFilterService.filterResponse(response);
        String sql = "INSERT INTO response (contenu,dateCreation,user_id,thread_id,likes,liked_by) VALUES (?,?,?,?,?,?)";
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pst.setString(1, response.getContenu());
            pst.setTimestamp(2, Timestamp.valueOf(response.getDateCreation()));
            pst.setInt(3, response.getUser().getId());
            pst.setInt(4, response.getThread().getThreadId());
            pst.setInt(5, 0); // initial likes
            pst.setString(6, ""); // initial liked_by
            pst.executeUpdate();

            ResultSet rs = pst.getGeneratedKeys();
            if (rs.next()) {
                response.setResponseId(rs.getInt(1));
            }

            System.out.println("✅ Response ajoutée");

            // ✅ CREATE NOTIFICATION for thread owner
            notificationService.notifyThreadOwner(response.getThread(), response);

        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override
    public void update(Response response) {
        String sql = "UPDATE response SET contenu=? WHERE response_id=?";
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, response.getContenu());
            pst.setInt(2, response.getResponseId());
            pst.executeUpdate();
            System.out.println("✅ Response modifiée");
        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM response WHERE response_id=?";
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, id);
            pst.executeUpdate();
            System.out.println("✅ Response supprimée");
        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override
    public List<Response> getAll() {
        List<Response> list = new ArrayList<>();
        String sql = "SELECT * FROM response";
        try (Connection conn = MyDataBase.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("user_id"));
                ForumThread thread = new ForumThread();
                thread.setThreadId(rs.getInt("thread_id"));
                list.add(new Response(
                        rs.getInt("response_id"),
                        rs.getString("contenu"),
                        rs.getTimestamp("dateCreation").toLocalDateTime(),
                        user, thread,
                        rs.getInt("likes"),
                        rs.getString("liked_by")
                ));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    // ✅ NEW: returns only responses for a specific thread
    public List<Response> getByThread(int threadId) {
        List<Response> list = new ArrayList<>();
        String sql = "SELECT * FROM response WHERE thread_id = ? ORDER BY dateCreation ASC";
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, threadId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    User user = new User();
                    user.setId(rs.getInt("user_id"));
                    ForumThread thread = new ForumThread();
                    thread.setThreadId(rs.getInt("thread_id"));
                    list.add(new Response(
                            rs.getInt("response_id"),
                            rs.getString("contenu"),
                            rs.getTimestamp("dateCreation").toLocalDateTime(),
                            user, thread,
                            rs.getInt("likes"),
                            rs.getString("liked_by")
                    ));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    public int countByThread(int threadId) {
        String sql = "SELECT COUNT(*) FROM response WHERE thread_id=?";
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, threadId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    /**
     * Like a response
     * @param responseId The response to like
     * @param userId The user who likes it
     * @return true if liked, false if already liked (unliked)
     */
    public boolean likeResponse(int responseId, int userId) {
        try (Connection conn = MyDataBase.getConnection()) {
            // Get current liked_by
            String selectSql = "SELECT liked_by, likes, thread_id FROM response WHERE response_id=?";
            String likedBy = "";
            int likes = 0;
            int threadId = 0;

            try (PreparedStatement pst = conn.prepareStatement(selectSql)) {
                pst.setInt(1, responseId);
                try (ResultSet rs = pst.executeQuery()) {
                    if (rs.next()) {
                        likedBy = rs.getString("liked_by");
                        if (likedBy == null) likedBy = "";
                        likes = rs.getInt("likes");
                        threadId = rs.getInt("thread_id");
                    }
                }
            }

            // Check if user already liked
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
                // Unlike: remove user from liked_by and decrement likes
                String[] likedUsers = likedBy.split(",");
                StringBuilder newLikedBy = new StringBuilder();
                for (String id : likedUsers) {
                    if (!id.equals(userIdStr) && !id.isEmpty()) {
                        if (newLikedBy.length() > 0) newLikedBy.append(",");
                        newLikedBy.append(id);
                    }
                }
                updateSql = "UPDATE response SET likes = likes - 1, liked_by = ? WHERE response_id=?";
                try (PreparedStatement pst = conn.prepareStatement(updateSql)) {
                    pst.setString(1, newLikedBy.toString());
                    pst.setInt(2, responseId);
                    pst.executeUpdate();
                }
                System.out.println("👎 Response unliked");
                return false;
            } else {
                // Like: add user to liked_by and increment likes
                String newLikedBy = likedBy.isEmpty() ? userIdStr : likedBy + "," + userIdStr;
                updateSql = "UPDATE response SET likes = likes + 1, liked_by = ? WHERE response_id=?";
                try (PreparedStatement pst = conn.prepareStatement(updateSql)) {
                    pst.setString(1, newLikedBy);
                    pst.setInt(2, responseId);
                    pst.executeUpdate();
                }
                System.out.println("👍 Response liked");

                // ✅ CREATE NOTIFICATION for response owner
                try {
                    // Get the full response object
                    Response response = null;
                    String getResponseSql = "SELECT * FROM response WHERE response_id=?";
                    try (PreparedStatement pst = conn.prepareStatement(getResponseSql)) {
                        pst.setInt(1, responseId);
                        try (ResultSet rs = pst.executeQuery()) {
                            if (rs.next()) {
                                User respUser = new User();
                                respUser.setId(rs.getInt("user_id"));
                                ForumThread respThread = new ForumThread();
                                respThread.setThreadId(rs.getInt("thread_id"));
                                response = new Response(
                                        rs.getInt("response_id"),
                                        rs.getString("contenu"),
                                        rs.getTimestamp("dateCreation").toLocalDateTime(),
                                        respUser, respThread,
                                        rs.getInt("likes"),
                                        rs.getString("liked_by")
                                );
                            }
                        }
                    }

                    if (response != null && threadId > 0) {
                        ForumThread thread = threadService.getById(threadId);
                        UserService userService = new UserService();
                        User likedByUser = userService.getUserById(userId);

                        if (thread != null && likedByUser != null) {
                            notificationService.notifyResponseLike(response, likedByUser, thread);
                        }
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

    /**
     * Check if a user has liked a response
     */
    public boolean hasUserLikedResponse(int responseId, int userId) {
        String sql = "SELECT liked_by FROM response WHERE response_id=?";
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, responseId);
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
}