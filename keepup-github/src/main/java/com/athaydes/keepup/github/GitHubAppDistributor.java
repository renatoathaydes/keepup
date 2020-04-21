package com.athaydes.keepup.github;

import com.athaydes.keepup.api.AppDistributor;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public class GitHubAppDistributor implements AppDistributor {
    private static final String GH_URL = "https://api.github.com/graphql";

    private static final String QUERY = "{\"query\":\"query{repository(owner:\\\"%s\\\",name:\\\"%s\\\"){" +
            "releases(last:1){nodes{tagName releaseAssets(first:%d){nodes{name downloadUrl}}}}}}\"}";

    private final String accessToken;
    private final String query;
    private final Predicate<String> isNewVersion;
    private final Function<GitHubResponse, GitHubAsset> selectDownloadAsset;

    private volatile GitHubResponse latestResponse;

    public GitHubAppDistributor(String accessToken,
                                String owner,
                                String repository,
                                int assetsCount,
                                Predicate<String> isNewVersion,
                                Function<GitHubResponse, GitHubAsset> selectDownloadAsset) {
        this.accessToken = accessToken;
        this.isNewVersion = isNewVersion;
        this.selectDownloadAsset = selectDownloadAsset;
        this.query = String.format(QUERY,
                validateQueryComponent(owner),
                validateQueryComponent(repository),
                assetsCount);
    }

    @Override
    public Optional<String> findLatestVersion() throws Exception {
        var connection = Http.connect(new URL(GH_URL), "POST");
        connection.setDoOutput(true);
        var payload = query.getBytes(StandardCharsets.UTF_8);
        connection.setRequestProperty("Authorization", "Bearer " + accessToken);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Content-Length", Integer.toString(payload.length));

        try {
            connection.getOutputStream().write(payload);
            if (connection.getResponseCode() == 200) {
                var response = GitHubResponse.fromGraphQL(connection.getInputStream());
                var latestVersion = response.getLatestVersion();
                if (isNewVersion.test(latestVersion)) {
                    latestResponse = response;
                    return Optional.of(latestVersion);
                }
            }
        } finally {
            connection.disconnect();
        }
        return Optional.empty();
    }

    @Override
    public File download(String version) throws Exception {
        var response = latestResponse;
        if (response == null) {
            throw new IllegalStateException("latestVersion is not known");
        }
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

    protected String validateQueryComponent(String s) {
        if (s.contains("\"")) {
            throw new IllegalArgumentException("contains invalid characters");
        }
        return s;
    }

}
