package au.gov.aims.atlasmapperserver.thread;

public abstract class AbstractConfigThread extends RevivableThread {
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
