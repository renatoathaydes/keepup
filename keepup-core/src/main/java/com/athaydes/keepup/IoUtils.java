package com.athaydes.keepup;

import com.athaydes.keepup.api.KeepupConfig;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

final class IoUtils {

    public static final String KEEPUP_UNPACKED_APP = "keepup-update";

    public static File unpack(File newVersionZipFile, File home) throws IOException {
        var destinationDir = unpackedApp(home);
        if (destinationDir.isDirectory()) {
            deleteContents(destinationDir);
        } else {
            if (destinationDir.isFile()) {
                throw new IllegalArgumentException("upgrade destination is not a directory, " +
                        "but a file: " + destinationDir);
            }
            if (!destinationDir.mkdirs()) {
                throw new IOException("upgrade destination directory cannot be created: " + destinationDir);
            }
        }

        try (var zip = new ZipInputStream(
                new BufferedInputStream(
                        new FileInputStream(newVersionZipFile), 4096))) {
            var zipEntry = zip.getNextEntry();
            if (zipEntry == null) {
                throw new IllegalStateException("Expected at least one entry in the zip file: " + newVersionZipFile);
            }
            var topEntryName = zipEntry.getName();
            zipEntry = zip.getNextEntry();
            while (zipEntry != null) {
                var file = fileFor(zipEntry, destinationDir, topEntryName);
                if (isDirectory(zipEntry)) {
                    var ok = file.mkdir();
                    if (!ok) throw new IOException("Cannot create new directory: " + file);
                } else {
                    Files.copy(zip, file.toPath());
                }
                zipEntry = zip.getNextEntry();
            }
        }

        return destinationDir;
    }

    static File unpackedApp(File home) {
        return new File(home, KEEPUP_UNPACKED_APP);
    }

    static File currentApp() {
        return new File(System.getProperty("java.home"));
    }

    private static File fileFor(ZipEntry zipEntry, File destinationDir, String topEntryName) {
        return new File(destinationDir, zipEntry.getName().substring(topEntryName.length()));
    }

    private static boolean isDirectory(ZipEntry zipEntry) {
        String name = zipEntry.getName();
        return name.endsWith("/") || name.endsWith("\\");
    }

    static void deleteContents(File dir) throws IOException {
        var files = dir.listFiles();
        if (files == null) throw new IllegalArgumentException("Not a directory: " + dir);

        for (File file : files) {
            Files.walkFileTree(file.toPath(), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    var ok = file.toFile().delete();
                    if (!ok) throw new IOException("Unable to delete file: " + file);
                    return super.visitFile(file, attrs);
                }

                @Override
                public FileVisitResult postVisitDirectory(Path directory, IOException exc) throws IOException {
                    var ok = directory.toFile().delete();
                    if (!ok) throw new IOException("Unable to delete directory: " + directory);
                    return super.postVisitDirectory(directory, exc);
                }
            });
        }
    }

    static void copy(File source, File destinationDir) throws IOException {
        Path srcPath = source.toPath();
        if (source.isFile()) {
            Files.copy(srcPath, destinationDir.toPath().resolve(srcPath));
        } else {
            Files.walkFileTree(srcPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (!srcPath.equals(dir)) {
                        var newDir = new File(destinationDir, srcPath.relativize(dir).toFile().getPath());
                        if (!newDir.exists()) {
                            var ok = newDir.mkdir();
                            if (!ok) {
                                throw new IOException("Cannot create directory " + newDir);
                            }
                        }
                    }
                    return super.preVisitDirectory(dir, attrs);
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    var newFile = destinationDir.toPath().resolve(srcPath.relativize(file).toFile().getPath());
                    Files.copy(file, newFile);
                    return super.visitFile(file, attrs);
                }
            });
        }
    }

    public static boolean looksLikeJlinkApp(File rootDir, KeepupConfig config) {
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

    public static void setFilePermissions(File rootDir, String appName) {
        new File(rootDir, "bin/" + appName).setExecutable(true, false);
        new File(rootDir, "bin/java").setExecutable(true, false);
        new File(rootDir, "bin/keytool").setExecutable(true, false);
        new File(rootDir, "lib/jexec").setExecutable(true, false);
        new File(rootDir, "lib/jspawnhelper").setExecutable(true, false);
    }

}
