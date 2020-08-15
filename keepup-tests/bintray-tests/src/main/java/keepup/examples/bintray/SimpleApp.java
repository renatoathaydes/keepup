package keepup.examples.bintray;

import com.athaydes.keepup.api.AppDistributor;
import com.athaydes.keepup.api.Keepup;
import com.athaydes.keepup.api.KeepupConfig;
import com.athaydes.keepup.api.UpdateInstaller;
import com.athaydes.keepup.bintray.BintrayAppDistributor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class SimpleApp {

    private static void log(String message) {
        try {
            Files.writeString(Paths.get("build", "app.log"), message + "\n",
                    StandardOpenOption.APPEND, StandardOpenOption.CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        log("SimpleApp version " + Version.CURRENT);

        var keepup = new Keepup(new SimpleConfig());

        keepup.onUpdate((newVersion, zipFile) -> {
            log("Updating to version " + newVersion);

            // Simple example where we always accept an update.
            // Custom implementations could
            // perform signature verification, verify the checksum,
            // ask the user if they want to update etc.
            // If none of that is required, you don't need to even set this
            // onUpdate callback!
            return CompletableFuture.completedFuture(true);
        }).onNoUpdateRequired(() -> log("No updates at this time"))
                .onError((e) -> log("Cannot update due to " + e))
                .onDone(
                        // done but no update: shutdown Keepup's executor if shouldn't check for updates again
                        keepup::shutdown,
                        // done with update successful: quit and launch the new version!
                        UpdateInstaller::quitAndLaunchUpdatedApp);

        // in this simple app, we just check for updates every time we start up...
        // When the app starts again after an update, Keepup will detect that and will
        // not actually check for new updates on the first call to this method.
        keepup.createUpdater().checkForUpdate();
    }
}

class SimpleConfig implements KeepupConfig {

    private static final String OS_FAMILY;

    static {
        String osName = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
        OS_FAMILY = osName.contains("windows")
                ? "win"
                : (osName.contains("mac") || osName.contains("darwin")
                ? "mac"
                : "linux");
    }

    @Override
    public String appName() {
        return "simple_app";
    }

    @Override
    public AppDistributor<?> distributor() {
        return new BintrayAppDistributor(
                // Bintray username
                "renatoathaydes",
                // Repository (normally includes the OS family as jlink images are OS-specific)
                OS_FAMILY,
                // name of the project
                "keepup-bintray-tests",
                // groupId of the module
                "com.athaydes.keepup",
                // artifactId of the module
                "bintray-tests",
                // accept version only if it's not the same as the current version
                (version) -> CompletableFuture.completedFuture(!Version.CURRENT.equals(version))
        );
    }

    @Override
    public ExecutorService executor() {
        return Executors.newSingleThreadExecutor();
    }

    @Override
    public File appHome() {
        return new File("build");
    }
}
