package au.gov.aims.atlasmapperserver.thread;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class ThreadLogger {
    private List<ThreadLog> logs;
    private int warningCount;
    private int errorCount;

    public ThreadLogger() {
        this.logs = new ArrayList<ThreadLog>();
        this.warningCount = 0;
        this.errorCount = 0;
    }

    public void reset() {
        this.logs.clear();
    }

    public List<ThreadLog> getLogs() {
        return this.logs;
    }

    public void log(Level level, String message) {
        this.log(new ThreadLog(level, message));
    }

    public void log(Level level, String message, Exception ex) {
        this.log(new ThreadLog(level, message, ex));
    }

    public void log(ThreadLog log) {
        this.logs.add(log);
        if (log.isError()) {
            this.errorCount++;
        }
        if (log.isWarning()) {
            this.warningCount++;
        }
    }

    public int getWarningCount() {
        return this.warningCount;
    }

    public int getErrorCount() {
        return this.errorCount;
    }

    public void addAll(ThreadLogger logger) {
        for (ThreadLog log : logger.logs) {
            this.log(log);
        }
    }

    public JSONArray toJSON() throws JSONException {
        JSONArray json = new JSONArray();

        for (ThreadLog log : this.logs) {
            json.put(log.toJSON());
        }

        return json;
    }
}
