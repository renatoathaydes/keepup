package com.athaydes.keepup.api;

import java.io.File;
import java.util.Optional;

/**
 * An AppDistributor defines how the application finds and downloads new releases.
 */
public interface AppDistributor {
    /**
     * Find the latest version of the application and return it if it is not the same as the
     * currently running application.
     *
     * @return the new version of the application, if any
     * @throws Exception on error
     */
    Optional<String> findLatestVersion() throws Exception;

    /**
     * Download or obtain a zip file for the given version of the application.
     *
     * @param version to download or obtain
     * @return zip file containing a jlink image. The zip file must contain a single root folder
     * under which the image resides.
     * @throws Exception on error
     */
    File download(String version) throws Exception;
}
