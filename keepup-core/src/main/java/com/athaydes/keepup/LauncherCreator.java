package com.athaydes.keepup;

import com.athaydes.keepup.api.KeepupConfig;
import com.athaydes.keepup.api.KeepupException;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import static com.athaydes.keepup.Unpacker.KEEPUP_UNPACKED_APP;
import static com.athaydes.keepup.api.KeepupException.ErrorCode.APP_HOME_NOT_WRITABLE;
import static com.athaydes.keepup.api.KeepupException.ErrorCode.NOT_JLINK_APP;

public final class LauncherCreator {
    public static void create(KeepupConfig config) throws Exception {
        var newVersionDir = new File(config.appHome(), KEEPUP_UNPACKED_APP);
        var appLocation = Paths.get(System.getProperty("java.home"));
        if (looksLikeJlinkApp(appLocation.toFile(), config)) {
            var ok = newVersionDir.mkdirs();
            if (!ok && !newVersionDir.isDirectory()) throw new KeepupException(APP_HOME_NOT_WRITABLE);
            var updateLauncher = new File(config.appHome(), updateLauncher(config));
            var launcher = appLocation.resolve(Paths.get("bin", config.appName()));
            Files.write(updateLauncher.toPath(), createScript(updateLauncher.toPath(), launcher, newVersionDir));
            updateLauncher.setExecutable(true);
        } else {
            throw new KeepupException(NOT_JLINK_APP);
        }
    }

    public static String updateLauncher(KeepupConfig config) {
        return config.appName() + "_update";
    }

    private static boolean looksLikeJlinkApp(File rootDir, KeepupConfig config) {
        if (rootDir.isDirectory()) {
            var children = rootDir.list();
            if (children != null && children.length >= 4) {
                if (Set.of(children).containsAll(Set.of("bin", "conf", "legal", "lib"))) {
                    return new File(rootDir, "bin/" + config.appName()).isFile();
                }
            }
        }
        return false;
    }

    private static byte[] createScript(Path updateLauncher,
                                       Path launcher,
                                       File newVersionDir) {
        var dir = launcher.getParent().getParent().toFile().getAbsolutePath();
        var newDir = newVersionDir.getAbsolutePath();
        var launcherPath = launcher.toFile().getAbsolutePath();
        var updateLauncherPath = updateLauncher.toFile().getAbsolutePath();

        return ("#!/bin/sh\n" +
                "\n" +
                "set -e\n" +
                "DIR=\"" + dir + "\"\n" +
                "rm -rf \"${DIR:?}\"/*\n" +
                "mv \"" + newDir + "\"/* \"${DIR:?}\"/\n" +
                "rm -r \"" + newDir + "\"\n" +
                "chmod +x " + launcherPath + "\n" +
                "exec " + launcherPath + "&" + "\n" +
                "rm " + updateLauncherPath +
                "\n").getBytes(StandardCharsets.UTF_8);
    }
}
