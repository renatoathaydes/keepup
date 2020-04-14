package com.athaydes.keepup.api;

import java.io.File;
import java.util.Optional;

public interface AppDistributor {
    Optional<String> findLatestVersion() throws Exception;

    File download(String version) throws Exception;
}
