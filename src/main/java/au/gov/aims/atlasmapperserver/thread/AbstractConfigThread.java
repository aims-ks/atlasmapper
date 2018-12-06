package au.gov.aims.atlasmapperserver.thread;

import java.text.DecimalFormat;

public abstract class AbstractConfigThread extends RevivableThread {
    // Used to format the elapse time (always put at lease 1 digit before the dot, with maximum 2 digits after)
    public static final DecimalFormat ELAPSE_TIME_FORMAT = new DecimalFormat("0.##");

    private ThreadLogger logger;

    public AbstractConfigThread() {
        this.logger = new ThreadLogger();
    }

    @Override
    public void reset() {
        this.logger.reset();
    }

    // Status

    public ThreadLogger getLogger() {
        return this.logger;
    }
}
