# Keepup

Keepup is a Java library for self-updating applications.

It is designed to work specifically with self-contained applications built with [jlink]().

## Requirements

Keepup has only the following requirements for an application to be able to use it:

* must be compiled with Java 11 or newer.
* must be packaged with `jlink`.
* the jlink-created image must be distributed in a zip file.

There must also be a mechanism for Keepup to know if there is a newer release. This is usually a HTTP endpoint
which provides the latest version, but can be anything, even a local file.  

## Features

Unlike most other self-update libraries, Keepup updates the application as a whole every time, including
the launcher, the JVM, all libraries, everything that is packed with `jlink` and included in the result image!

* works with any app launcher (no custom launcher required).
* upgrades the app and the JVM itself.
* works with any kind of application: CLI, Server, Desktop App (incl. JavaFX).
* application decides when to check for updates, and when to apply it.
* application can perform any check required in the distributed zip file.
* supports immediate app restart and upgrade on exit.
* does not enforce where app is distributed from: use GitHub Releases, Amazon S3, GitLab, your own server or anything else.  
* 100% configured with code.

## How to use it

An application must create an instance of the `Keepup` class to use Keepup. This requires two things:

### Implement `com.athaydes.keepup.api.AppDistributor`

> In the future, there will be some default implementations using common distribution channels,
> such as JCenter and GitHub Releases.

```java
import com.athaydes.keepup.api.AppDistributor;
import java.util.Optional;

class MyAppDistributor implements AppDistributor {

    @Override
    public Optional<String> findLatestVersion() throws Exception {
        // fetch the new version from somewhere...
        // only return non-empty if the version is not the same as the current one!
        return Optional.of("v2");
    }

    @Override
    public File download(String version) throws Exception {
        // download the zip file for the new version...
        return new File("my_app.zip");
    }
}
```

### Implement `com.athaydes.keepup.api.KeepupConfig`

```java
import com.athaydes.keepup.api.KeepupConfig;

class TrivialConfig implements KeepupConfig {

    @Override
    public String appName() {
        return "my-app";
    }

    @Override
    public AppDistributor distributor() {
        return new MyAppDistributor();
    }
}
```

Now, create a `Keepup` instance and set callbacks for everything:

> All callbacks have sensible defaults, but you probably should implement them anyway
> to customize how your users will be told about upgrades, how to validate the download, etc.

```java
class MyApp {

    public static void main(String[] args) {
        var keepup = new Keepup(new MyConfig());

        keepup.onUpdate((newVersion, zipFile) -> {
            // Simple example where we always accept an update.
            // Custom implementations could
            // perform signature verification, verify the checksum,
            // ask the user if they want to update etc.
            // If none of that is required, you don't need to even set this
            // onUpdate callback!
            return CompletableFuture.completedFuture(true);
        }).onNoUpdateRequired(() -> {
            System.out.println("No updates at this time");
        }).onError((e) -> {
            System.err.println("Cannot update due to " + e);
        }).onDone((installer) -> {
            // the installer is only provided on a successful update...
            // on errors or not-accepted updates, it would be null
            if (installer != null) {
                // call one of:
                // * quitAndLaunchUpgradedApp()
                // * launchUpgradedAppWithoutExiting()
                // * installUpgradeOnExit()
                installer.quitAndLaunchUpgradedApp();
            }

            // close Keepup if you do not need to check again
            keepup.close();
        });

        // decide when to check for updates...
        keepup.createUpdater().checkForUpdate();
    }
}
```
