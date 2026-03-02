package service;

import entities.*;
import utils.MyDataBase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MediaService {

    public void add(Media media) {
        String sql = "INSERT INTO media (url,type,thread_id,response_id) VALUES (?,?,?,?)";
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, media.getUrl());
            pst.setString(2, media.getType().name());
            if (media.getThread() != null) pst.setInt(3, media.getThread().getThreadId());
            else pst.setNull(3, Types.INTEGER);
            if (media.getResponse() != null) pst.setInt(4, media.getResponse().getResponseId());
            else pst.setNull(4, Types.INTEGER);
            pst.executeUpdate();
            System.out.println("✅ Media ajouté");
        } catch (Exception e) { e.printStackTrace(); }
    }

    public List<Media> getMediaByThread(int threadId) {
        List<Media> list = new ArrayList<>();
        String sql = "SELECT * FROM media WHERE thread_id=?";
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, threadId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    ForumThread thread = new ForumThread();
                    thread.setThreadId(threadId);
                    list.add(new Media(
                            rs.getInt("media_id"),
                            rs.getString("url"),
                            MediaType.valueOf(rs.getString("type")),
                            thread, null
                    ));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    public List<Media> getMediaByResponse(int responseId) {
        List<Media> list = new ArrayList<>();
        String sql = "SELECT * FROM media WHERE response_id=?";
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, responseId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    Response response = new Response();
                    response.setResponseId(responseId);
                    list.add(new Media(
                            rs.getInt("media_id"),
                            rs.getString("url"),
                            MediaType.valueOf(rs.getString("type")),
                            null, response
                    ));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    public void delete(int mediaId) {
        String sql = "DELETE FROM media WHERE media_id=?";
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, mediaId);
            pst.executeUpdate();
            System.out.println("✅ Media supprimé");
        } catch (Exception e) { e.printStackTrace(); }
    }
}