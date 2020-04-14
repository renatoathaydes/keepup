package com.athaydes.keepup.api;

import java.io.File;
import java.util.concurrent.ExecutorService;

public interface KeepupConfig {
    String appName();

    default File appHome() {
        return new File(System.getProperty("user.home"), "." + appName());
    }

    AppDistributor distributor();

    ExecutorService executor();
}
