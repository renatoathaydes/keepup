package com.athaydes.keepup.github;

import com.athaydes.keepup.api.AppDistributor;
import com.athaydes.keepup.api.KeepupException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static com.athaydes.keepup.api.KeepupException.ErrorCode.LATEST_VERSION_CHECK;

/**
 * An implementation of {@link AppDistributor} based on GitHub Releases.
 * <p>
 * It uses the <a href="https://developer.github.com/v4/">GitHub GraphQL API (version 4)</a>
 * to obtain data about a repository's releases.
 * <p>
 * Most behaviour of this class is customizable via callbacks passed into its constructor, so it is usually
 * not necessary to sub-class it to adapt its behaviour.
 * <p>
 * This implementation avoids relying on anything other than the Java basic standard library
 * (module {@code java.base}) and a small JSON parser (org.json) by performing HTTP requests via
 * {@link java.net.URLConnection}, and building the GraphQL query (which is static) as a simple JSON
 * String.
 */
public class GitHubAppDistributor implements AppDistributor<GithubAppVersion> {
    private static final String GH_URL = "https://api.github.com/graphql";

    private static final String QUERY = "{\"query\":\"query{repository(owner:\\\"%s\\\",name:\\\"%s\\\"){" +
            "releases(last:1){nodes{tagName releaseAssets(first:%d){nodes{name downloadUrl}}}}}}\"}";

    private final String accessToken;
    private final String query;
    private final Function<String, CompletionStage<Boolean>> acceptVersion;
    private final Function<GitHubResponse, GitHubAsset> selectDownloadAsset;

    public GitHubAppDistributor(String accessToken,
                                String owner,
                                String repository,
                                int assetsCount,
                                Function<String, CompletionStage<Boolean>> acceptVersion,
                                Function<GitHubResponse, GitHubAsset> selectDownloadAsset) {
        this.accessToken = accessToken;
        this.acceptVersion = acceptVersion;
        this.selectDownloadAsset = selectDownloadAsset;
        this.query = String.format(QUERY,
                validateQueryComponent(owner),
                validateQueryComponent(repository),
                assetsCount);
    }

    @Override
    public CompletionStage<Optional<GithubAppVersion>> findLatestVersion() throws Exception {
        var connection = Http.connect(new URL(GH_URL), "POST");
        connection.setDoOutput(true);
        var payload = query.getBytes(StandardCharsets.UTF_8);
        connection.setRequestProperty("Authorization", "Bearer " + accessToken);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Content-Length", Integer.toString(payload.length));

        var future = new CompletableFuture<Optional<GithubAppVersion>>();
        try {
            connection.getOutputStream().write(payload);
            if (connection.getResponseCode() == 200) {
                var response = GitHubResponse.fromGraphQL(
                        new BufferedInputStream(connection.getInputStream(), 4096));
                var latestVersion = response.getLatestVersion();
                acceptVersion.apply(latestVersion)
                        .whenComplete(completeFuture(future, response));
                return future;
            } else {
                var error = new String(connection.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                future.completeExceptionally(new KeepupException(
                        LATEST_VERSION_CHECK, String.format(
                        "GitHub response status code is not 200: %s - body: %s",
                        connection.getResponseCode(),
                        error)));
                return future;
            }
        } finally {
            connection.disconnect();
        }
    }

    private BiConsumer<Boolean, Throwable> completeFuture(CompletableFuture<Optional<GithubAppVersion>> future,
                                                          GitHubResponse response) {
        return (accept, error) -> {
            if (error != null) {
                future.completeExceptionally(error);
            } else {
                if (accept) {
                    future.complete(Optional.of(new GithubAppVersion(response)));
                } else {
                    future.complete(Optional.empty());
                }
            }
        };
    }

    @Override
    public File download(GithubAppVersion version) throws Exception {
        var response = version.getResponse();
        var asset = selectDownloadAsset.apply(response);
        var connection = Http.connect(asset.getUri().toURL(), "GET");
        try {
            if (connection.getResponseCode() != 200) {
                throw new IOException("Unexpected status code: " + connection.getResponseCode());
            }
            var file = Files.createTempFile("keepup-asset", ".zip");
            Files.copy(connection.getInputStream(), file, StandardCopyOption.REPLACE_EXISTING);
            return file.toFile();
        } finally {
            connection.disconnect();
        }
    }

    /**
     * Check value to be passed into a GraphQL query.
     * <p>
     * The default implementation forbids double-quotes as that could break the JSON object
     * representing the query.
     *
     * @param value for a GraphQL query component
     * @return the validated, possibly santized value
     */
    protected String validateQueryComponent(String value) {
        if (value.contains("\"")) {
            throw new IllegalArgumentException("contains invalid characters");
        }
        return value;
    }

}
