package com.athaydes.keepup.api;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

final class KeepupConfigWrapper implements KeepupConfig {
    private final String appName;
    private final File appHome;
    private final AppDistributor distributor;
    private final ExecutorService executorService;
    private final Consumer<String> logger;

    public KeepupConfigWrapper(KeepupConfig delegate) {
        this.appName = delegate.appName();
        this.appHome = delegate.appHome();
        this.distributor = delegate.distributor();
        this.executorService = delegate.executor();
        this.logger = delegate.logger();
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

    @Override
    public Consumer<String> logger() {
        return logger;
    }
}
