package com.athaydes.keepup.github;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * An GitHub API response for a request for release information about a particular repository.
 * <p>
 * It includes the actual version of the release, as well as the release's available assets.
 */
public final class GitHubResponse {
    private final String latestVersion;
    private final List<GitHubAsset> assets;

    public GitHubResponse(String latestVersion, List<GitHubAsset> assets) {
        this.latestVersion = latestVersion;
        this.assets = assets;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public List<GitHubAsset> getAssets() {
        return assets;
    }

    static GitHubResponse fromGraphQL(InputStream stream) throws IOException {
        var obj = new JSONObject(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        var errors = obj.has("errors") ? obj.getJSONArray("errors") : null;
        if (errors != null && errors.length() > 0) {
            throw new IllegalArgumentException("Errors: " + errors.join(", "));
        }

        var latestRelease = obj.getJSONObject("data")
                .getJSONObject("repository")
                .getJSONObject("releases")
                .getJSONArray("nodes")
                .getJSONObject(0);

        var latestVersion = latestRelease.getString("tagName");
        var assets = latestRelease.getJSONObject("releaseAssets")
                .getJSONArray("nodes");

        return new GitHubResponse(latestVersion, GitHubAsset.fromJSON(assets));
    }
}
