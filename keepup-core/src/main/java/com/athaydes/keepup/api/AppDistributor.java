package com.athaydes.keepup.api;

import java.io.File;
import java.util.Optional;

/**
 * An AppDistributor defines how the application finds and downloads new releases.
 *
 * @param <V> type of version object, which can contain extra information that the
 *            {@link AppDistributor#download(AppVersion)} method needs.
 * @see AppVersion
 */
public interface AppDistributor<V extends AppVersion> {
    /**
     * Find the latest version of the application and return it if it is not the same as the
     * currently running application.
     *
     * @return the new version of the application, if any
     * @throws Exception on error
     */
    Optional<V> findLatestVersion() throws Exception;

    /**
     * Download or obtain a zip file for the given version of the application.
     *
     * @param version to download or obtain
     * @return zip file containing a jlink image. The zip file must contain a single root folder
     * under which the image resides.
     * @throws Exception on error
     */
    File download(V version) throws Exception;
}
