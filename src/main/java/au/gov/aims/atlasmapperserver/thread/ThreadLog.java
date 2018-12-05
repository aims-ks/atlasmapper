package au.gov.aims.atlasmapperserver.thread;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.logging.Level;

public class ThreadLog {
    private Level level;
    private String message;
    private Exception exception;

    public ThreadLog(Level level, String message) {
        this(level, message, null);
    }

    public ThreadLog(Level level, String message, Exception exception) {
        this.level = level;
        this.message = message;
        this.exception = exception;
    }

    public Level getLevel() {
        return this.level;
    }

    public String getMessage() {
        return this.message;
    }

    public Exception getException() {
        return this.exception;
    }

    public boolean isError() {
        return Level.SEVERE.equals(this.level);
    }

    public boolean isWarning() {
        return Level.WARNING.equals(this.level);
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();

        json.put("level", this.level.getName());
        json.put("message", this.getMessage());

        return json;
    }
}
