package keepup.examples.simple;

import com.athaydes.keepup.api.AppDistributor;
import com.athaydes.keepup.api.Keepup;
import com.athaydes.keepup.api.KeepupConfig;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
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
        }).onNoUpdateRequired(() -> {
            log("No updates at this time");
        }).onError((e) -> {
            log("Cannot update due to " + e);
        }).onDone((installer) -> {
            // the installer is only provided on a successful update...
            // on errors or not-accepted updates, it would be null
            if (installer != null) {
                installer.quitAndLaunchUpgradedApp();
            }

            // close Keepup as we do not check for updates again
            keepup.close();
        });

        // in this simple app, we just check for updates every time we start up...
        // When the app starts again after an update, Keepup will detect that and will
        // not actually check for new updates on the first call to this method.
        keepup.createUpdater().checkForUpdate();
    }
}

class SimpleConfig implements KeepupConfig {

    @Override
    public String appName() {
        return "simple_app";
    }

    @Override
    public AppDistributor distributor() {
        // silly implementation that updates every time to the same version
        return new SimpleAppDistributor();
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

class SimpleAppDistributor implements AppDistributor {
    final File newVersionZip = new File("build", "simple-app.zip");

    @Override
    public Optional<String> findLatestVersion() {
        return newVersionZip.isFile() ? Optional.of("v2") : Optional.empty();
    }

    @Override
    public File download(String version) {
        // no need to download, just expect a local file to contain the new version
        return newVersionZip;
    }
}