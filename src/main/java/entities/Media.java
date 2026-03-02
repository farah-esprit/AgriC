package entities;

public class Media {

    private int mediaId;
    private String url;
    private MediaType type;
    private ForumThread thread;    // facultatif
    private Response response;     // facultatif

    public Media() {}

    public Media(int mediaId, String url, MediaType type, ForumThread thread, Response response) {
        this.mediaId = mediaId;
        this.url = url;
        this.type = type;
        this.thread = thread;
        this.response = response;
    }

    public Media(String url, MediaType type, ForumThread thread, Response response) {
        this.url = url;
        this.type = type;
        this.thread = thread;
        this.response = response;
    }

    // Getters & Setters
    public int getMediaId() { return mediaId; }
    public void setMediaId(int mediaId) { this.mediaId = mediaId; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public MediaType getType() { return type; }
    public void setType(MediaType type) { this.type = type; }

    public ForumThread getThread() { return thread; }
    public void setThread(ForumThread thread) { this.thread = thread; }

    public Response getResponse() { return response; }
    public void setResponse(Response response) { this.response = response; }
}
