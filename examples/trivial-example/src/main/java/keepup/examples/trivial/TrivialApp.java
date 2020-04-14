package keepup.examples.trivial;

import com.athaydes.keepup.api.AppDistributor;
import com.athaydes.keepup.api.Keepup;
import com.athaydes.keepup.api.KeepupConfig;

import java.io.File;
import java.util.Optional;

class TrivialApp {
    public static void main(String[] args) {
        System.out.println("Trivial App running.");

        var keepup = new Keepup(new TrivialConfig());

        // in this simple app, we just check for updates every time we start up...
        // When the app starts again after an update, Keepup will detect that and will
        // not actually check for new updates on the first call to this method.
        keepup.checkForUpdate();
    }
}

class TrivialConfig implements KeepupConfig {

    @Override
    public String appName() {
        return "my_app";
    }

    @Override
    public AppDistributor distributor() {
        // silly implementation that updates every time to the same version
        return new TrivialAppDistributor();
    }
}

class TrivialAppDistributor implements AppDistributor {

    @Override
    public Optional<String> findLatestVersion() {
        // we always return a new version!
        return Optional.of("v2");
    }

    @Override
    public File download(String version) {
        // no need to download, just expect a local file to contain the new version
        return new File("build", "trivial-app.zip");
    }
}