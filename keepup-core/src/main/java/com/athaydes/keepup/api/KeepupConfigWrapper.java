package com.athaydes.keepup.api;

import java.io.File;
import java.util.concurrent.ExecutorService;

final class KeepupConfigWrapper implements KeepupConfig {
    private final String appName;
    private final File appHome;
    private final AppDistributor distributor;
    private final ExecutorService executorService;

    public KeepupConfigWrapper(KeepupConfig delegate) {
        this.appName = delegate.appName();
        this.appHome = delegate.appHome();
        this.distributor = delegate.distributor();
        this.executorService = delegate.executor();
    }

    @Override
    public String appName() {
        return appName;
    }

    @Override
    public File appHome() {
        return appHome;
    }

    @Override
    public AppDistributor distributor() {
        return distributor;
    }

    @Override
    public ExecutorService executor() {
        return executorService;
    }
}
