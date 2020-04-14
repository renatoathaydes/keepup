package com.athaydes.keepup;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class Unpacker {

    public static final String KEEPUP_UNPACKED_APP = "keepup-update";

    public static void unpack(File newVersionZipFile, File home) throws IOException {
        var destinationDir = unpackedApp(home);
        if (destinationDir.isDirectory()) {
            deleteContents(destinationDir);
        } else {
            if (destinationDir.isFile()) {
                throw new IllegalArgumentException("app home is not a directory, but a file");
            }
            destinationDir.mkdirs();
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
                    if (!ok) throw new IllegalStateException("Cannot create new directory: " + file);
                } else {
                    Files.copy(zip, file.toPath());
                }
                zipEntry = zip.getNextEntry();
            }
        }
    }

    static File unpackedApp(File home) {
        return new File(home, KEEPUP_UNPACKED_APP);
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
        if (files == null) throw new IllegalStateException("Not a directory: " + dir);

        for (File file : files) {
            Files.walkFileTree(file.toPath(), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    var ok = file.toFile().delete();
                    if (!ok) throw new IllegalStateException("Unable to delete file: " + file);
                    return super.visitFile(file, attrs);
                }

                @Override
                public FileVisitResult postVisitDirectory(Path directory, IOException exc) throws IOException {
                    var ok = directory.toFile().delete();
                    if (!ok) throw new IllegalStateException("Unable to delete directory: " + directory);
                    return super.postVisitDirectory(directory, exc);
                }
            });
        }
    }

}
