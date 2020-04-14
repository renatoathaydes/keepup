package com.athaydes.keepup.api;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public interface KeepupConfig {
    String appName();

    default File appHome() {
        return new File(System.getProperty("user.home"), "." + appName());
    }

    AppDistributor distributor();

    default ExecutorService executor() {
        return Executors.newSingleThreadExecutor();
    }
}
