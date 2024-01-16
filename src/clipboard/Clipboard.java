package clipboard;

public class Clipboard {
    private static Clipboard instance;

    private String content = "";

    private Clipboard() {}

    public static Clipboard getInstance() {
        if (instance == null) {
            instance = new Clipboard();
        }
        return instance;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
