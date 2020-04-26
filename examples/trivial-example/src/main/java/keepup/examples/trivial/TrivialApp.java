package keepup.examples.trivial;

import com.athaydes.keepup.api.AppDistributor;
import com.athaydes.keepup.api.AppVersion;
import com.athaydes.keepup.api.Keepup;
import com.athaydes.keepup.api.KeepupConfig;
import com.athaydes.keepup.api.UpdateInstaller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

class TrivialApp {

    private static void log(String message) throws IOException {
        Files.writeString(Paths.get("build", "app.log"), message + "\n",
                StandardOpenOption.APPEND, StandardOpenOption.CREATE);
    }

    public static void main(String[] args) throws IOException {
        log("TrivialApp version " + Version.CURRENT);

        var keepup = new Keepup(new TrivialConfig());

        keepup.onDone(
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

class TrivialConfig implements KeepupConfig {

    @Override
    public String appName() {
        return "trivial_app";
    }

    @Override
    public AppDistributor<?> distributor() {
        // silly implementation that updates every time to the same version
        return new TrivialAppDistributor();
    }
}

class TrivialAppDistributor implements AppDistributor<AppVersion> {
    final File newVersionZip = new File("build", "trivial-app.zip");

    @Override
    public CompletionStage<Optional<AppVersion>> findLatestVersion() {
        return CompletableFuture.completedFuture(newVersionZip.isFile()
                ? Optional.of(AppVersion.ofString("v2"))
                : Optional.empty());
    }

    @Override
    public File download(AppVersion version) {
        // no need to download, just expect a local file to contain the new version
        return newVersionZip;
    }
}