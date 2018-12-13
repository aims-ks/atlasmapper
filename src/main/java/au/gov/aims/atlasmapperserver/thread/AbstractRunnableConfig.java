package au.gov.aims.atlasmapperserver.thread;

import au.gov.aims.atlasmapperserver.AbstractConfig;
import au.gov.aims.atlasmapperserver.ConfigManager;

public abstract class AbstractRunnableConfig<T extends AbstractConfigThread> extends AbstractConfig {
    protected T configThread;

    public AbstractRunnableConfig (ConfigManager configManager, T configThread) {
        super(configManager);
        this.configThread = configThread;
    }

    public synchronized void start() {
        if (this.isIdle()) {
            this.configThread.start();
        }
    }

    public synchronized void stop() {
        if (!this.isIdle()) {
            this.configThread.interrupt();
        }
    }

    public boolean isIdle() {
        return this.configThread == null || !this.configThread.isAlive();
    }

    public T getThread() {
        return this.configThread;
    }

    public void setThread(T configThread) {
        this.configThread = configThread;
    }
}
