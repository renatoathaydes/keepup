package com.athaydes.keepup.api;

/**
 * The version of an application.
 * <p>
 * If only a String is enough to represent a version,
 * use the {@link AppVersion#ofString(String)} method.
 */
public interface AppVersion {
    /**
     * @return name of the application version
     */
    String name();

    /**
     * Wrap a String into a {@link AppVersion} instance.
     *
     * @param version name of version
     * @return wrapped version
     */
    static AppVersion ofString(String version) {
        return () -> version;
    }
}
