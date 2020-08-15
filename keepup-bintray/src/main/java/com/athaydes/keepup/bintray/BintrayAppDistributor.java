package com.athaydes.keepup.bintray;

import com.athaydes.keepup.api.AppDistributor;
import com.athaydes.keepup.api.KeepupException;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static com.athaydes.keepup.api.KeepupException.ErrorCode.LATEST_VERSION_CHECK;

public class BintrayAppDistributor implements AppDistributor<BintrayVersion> {
    private final String subject;
    private final String repositoryName;
    private final String packageName;
    private final String groupId;
    private final String artifactId;
    private final Function<String, CompletionStage<Boolean>> acceptVersion;

    public BintrayAppDistributor(String subject,
                                 String repositoryName,
                                 String packageName,
                                 String groupId,
                                 String artifactId,
                                 Function<String, CompletionStage<Boolean>> acceptVersion) {
        this.subject = subject;
        this.repositoryName = repositoryName;
        this.packageName = packageName;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.acceptVersion = acceptVersion;
    }

    @Override
    public CompletionStage<Optional<BintrayVersion>> findLatestVersion() throws Exception {
        var url = new URL(String.format("https://bintray.com/%s/%s/%s/_latestVersion",
                subject, repositoryName, packageName));

        var future = new CompletableFuture<Optional<BintrayVersion>>();

        var con = Http.connect(url, "HEAD", false);
        try {
            int status = con.getResponseCode();

            String error;
            if (300 <= status && status <= 310) {
                String location = con.getHeaderField("Location");
                if (location != null) {
                    String latestVersion;
                    try {
                        latestVersion = extractVersionFromLocation(location);
                        acceptVersion.apply(latestVersion)
                                .whenComplete(completeFuture(future, new BintrayVersion(latestVersion)));

                        // success, return early
                        return future;
                    } catch (KeepupException e) {
                        error = e.getMessage();
                    }
                } else {
                    error = "Redirect Location is missing";
                }
            } else {
                error = "Unexpected status code: " + status;
            }

            future.completeExceptionally(new KeepupException(LATEST_VERSION_CHECK, error));
        } catch (IOException e) {
            future.completeExceptionally(new KeepupException(LATEST_VERSION_CHECK, e.toString()));
        } finally {
            con.disconnect();
        }

        return future;
    }

    @Override
    public File download(BintrayVersion version) throws Exception {
        var url = new URL(String.format("https://dl.bintray.com/%s/%s/%s/%s/%s/%s-%s.zip",
                subject, repositoryName, coordToPath(groupId), artifactId,
                version.name(), artifactId, version.name()));

        var connection = Http.connect(url, "GET", true);

        try {
            if (connection.getResponseCode() != 200) {
                throw new IOException("Unexpected status code: " + connection.getResponseCode() +
                        " (" + url + ")");
            }
            var file = Files.createTempFile("keepup-asset", ".zip");
            Files.copy(connection.getInputStream(), file, StandardCopyOption.REPLACE_EXISTING);
            return file.toFile();
        } finally {
            connection.disconnect();
        }
    }

    private static String extractVersionFromLocation(String location) {
        URI uri = URI.create(location);
        String path = uri.getPath();
        int idx = path.lastIndexOf('/');
        if (idx > 0 && idx < path.length() - 1) {
            return path.substring(idx + 1);
        } else {
            throw new KeepupException(LATEST_VERSION_CHECK,
                    "Unexpected URL path, does not contain a version: " + location);
        }
    }

    private static BiConsumer<Boolean, Throwable> completeFuture(CompletableFuture<Optional<BintrayVersion>> future,
                                                                 BintrayVersion bintrayVersion) {
        return (accept, error) -> {
            if (error != null) {
                future.completeExceptionally(error);
            } else {
                if (accept) {
                    future.complete(Optional.of(bintrayVersion));
                } else {
                    future.complete(Optional.empty());
                }
            }
        };
    }

    private static String coordToPath(String coordinate) {
        return coordinate.replaceAll("\\.", "/");
    }

}
