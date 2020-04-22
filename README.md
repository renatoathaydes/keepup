# Keepup

![Keepup Tests](https://github.com/renatoathaydes/keepup/workflows/Keepup%20Tests/badge.svg)

Keepup is a library to allow Java 11+ applications to self-update.

It is designed to work specifically with self-contained applications built with [jlink](https://docs.oracle.com/en/java/javase/11/tools/jlink.html).

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
* no bash or batch code involved: Keepup is pure Java.

## Using Keepup

### Dependency setup

Gradle:

```groovy
implementation "com.athaydes.keepup:keepup-core:1.0"
```

Maven:

```xml
<dependency>
    <groupId>com.athaydes.keepup</groupId>
    <artifactId>keepup-core</artifactId>
    <version>1.0</version>
</dependency>
```

### Module setup

```java
module my.mod {
    requires com.athaydes.keepup.core;
}
```

### Code

An application must create an instance of the `Keepup` class to use Keepup. This requires two things:

#### Implement `com.athaydes.keepup.api.AppDistributor`

> Currently, Keepup has one ready-to-use implementation of `AppDistributor` in the 
> [keepup-github](keepup-github) module, which uses the GitHub Releases API to find and download
> new versions. In the future, there should be more implementations, like for Maven Central or JCenter.

```java
import com.athaydes.keepup.api.AppDistributor;
import com.athaydes.keepup.api.AppVersion;
import java.io.File;
import java.util.Optional;

class MyAppDistributor implements AppDistributor<AppVersion> {

    @Override
    public Optional<AppVersion> findLatestVersion() throws Exception {
        // fetch the new version from somewhere...
        // only return non-empty if the version is not the same as the current one!
        return Optional.of(AppVersion.ofString("v2"));
    }

    @Override
    public File download(AppVersion version) throws Exception {
        // download the zip file for the new version...
        return new File("my_app.zip");
    }
}
```

#### Implement `com.athaydes.keepup.api.KeepupConfig`

```java
import com.athaydes.keepup.api.AppDistributor;
import com.athaydes.keepup.api.KeepupConfig;

class TrivialConfig implements KeepupConfig {

    @Override
    public String appName() {
        return "my-app";
    }

    @Override
    public AppDistributor<?> distributor() {
        return new MyAppDistributor();
    }
}
```

#### Create a `Keepup` instance and set callbacks

Now, create a `Keepup` instance and set callbacks for everything.

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

## Working examples

Please find working examples of applications using Keepup in the [examples](examples) directory.
