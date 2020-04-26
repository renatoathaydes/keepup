package com.athaydes.keepup.github;

import org.json.JSONArray;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A release asset available for download from the GitHub API.
 */
public final class GitHubAsset {
    private final String name;
    private final URI uri;

    public GitHubAsset(String name, URI uri) {
        this.name = name;
        this.uri = uri;
    }

    public String getName() {
        return name;
    }

    public URI getUri() {
        return uri;
    }

    static List<GitHubAsset> fromJSON(JSONArray assets) {
        var result = new ArrayList<GitHubAsset>(assets.length());
        for (int i = 0; i < assets.length(); i++) {
            var asset = assets.getJSONObject(i);
            result.add(new GitHubAsset(asset.getString("name"),
                    URI.create(asset.getString("downloadUrl"))));
        }
        return Collections.unmodifiableList(result);
    }
}
