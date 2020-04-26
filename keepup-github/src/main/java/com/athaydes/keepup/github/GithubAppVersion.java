package com.athaydes.keepup.github;

import com.athaydes.keepup.api.AppVersion;

/**
 * A version of an application provided by the GitHub API.
 */
public final class GithubAppVersion implements AppVersion {
    private final GitHubResponse response;

    public GithubAppVersion(GitHubResponse response) {
        this.response = response;
    }

    @Override
    public String name() {
        return response.getLatestVersion();
    }

    public GitHubResponse getResponse() {
        return response;
    }
}
