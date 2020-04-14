package keepup.examples.simple;

import com.athaydes.keepup.api.AppDistributor;
import com.athaydes.keepup.api.Keepup;
import com.athaydes.keepup.api.KeepupConfig;

import java.io.File;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class SimpleApp {
    public static void main(String[] args) {
        var keepup = new Keepup(new SimpleConfig());
        // close Keepup so that it shuts-down the executor
        keepup.onUpdate((newVersion, zipFile) -> {
            System.out.println("Updating to version " + newVersion);

            // Simple example where we just unpack the app update and do nothing else.
            // Custom implementations could
            // perform signature verification, verify the checksum,
            // ask the user if they want to update etc.
            // If none of that is required, you don't need to even set this
            // onUpdate callback! But if set, you must call keepup.unpack()
            // to proceed with the update.
            keepup.unpack(zipFile).thenRun(() -> {
                System.out.println("A new update has been installed. Restarting...");
                keepup.restartApplication();
            });
        }).onNoUpdateRequired(() -> {
            System.out.println("No updates at this time");
        }).onError((e) -> {
            System.err.println("Cannot update due to " + e);
        });

        // in this simple app, we just check for updates every time we start up...
        // When the app starts again after an update, Keepup will detect that and will
        // not actually check for new updates on the first call to this method.
        keepup.checkForUpdate();
    }
}

class SimpleConfig implements KeepupConfig {

    @Override
    public String appName() {
        return "my_app";
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

    @Override
    public Optional<String> findLatestVersion() {
        // we always return a new version!
        return Optional.of("v2");
    }

    @Override
    public File download(String version) {
        // no need to download, just expect a local file to contain the new version
        return new File("build", "simple-local-distro.zip");
    }
}