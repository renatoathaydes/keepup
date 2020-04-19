package keepup.examples.trivial;

import com.athaydes.keepup.api.AppDistributor;
import com.athaydes.keepup.api.Keepup;
import com.athaydes.keepup.api.KeepupConfig;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

class TrivialApp {

    private static void log(String message) throws IOException {
        Files.writeString(Paths.get("build", "app.log"), message + "\n",
                StandardOpenOption.APPEND, StandardOpenOption.CREATE);
    }

    public static void main(String[] args) throws IOException {
        log("TrivialApp version " + Version.CURRENT);

        var keepup = new Keepup(new TrivialConfig());

        keepup.onDone((installer) -> {
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

class TrivialConfig implements KeepupConfig {

    @Override
    public String appName() {
        return "trivial_app";
    }

    @Override
    public AppDistributor distributor() {
        // silly implementation that updates every time to the same version
        return new TrivialAppDistributor();
    }
}

class TrivialAppDistributor implements AppDistributor {
    final File newVersionZip = new File("build", "trivial-app.zip");

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