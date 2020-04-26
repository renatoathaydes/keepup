# Keepup-github

Keepup-github is a Java module providing an implementation of `com.athaydes.keepup.api.AppDistributor`
that utilizes the [GitHub GraphQL API](https://developer.github.com/v4/) to obtain new versions of an application.

The implementation class is called `com.athaydes.keepup.github.GitHubAppDistributor` and can be used in a
`KeepupConfig` as follows:

```java
import com.athaydes.keepup.github.GitHubAppDistributor;

public class MyKeepupConfig implements KeepupConfig {
    
    private static final int MAX_ASSETS = 3;

    private final Function<String, CompletionStage<Boolean>> acceptVersion;
    private final Function<GitHubResponse, GitHubAsset> selectDownloadAsset;

    public MyKeepupConfig( Function<String, CompletionStage<Boolean>> acceptVersion,
                           Function<GitHubResponse, GitHubAsset> selectDownloadAsset ) {
        this.acceptVersion = acceptVersion;
        this.selectDownloadAsset = selectDownloadAsset;
    }

    @Override
    public String appName() {
        return "app_name";
    }

    @Override
    public AppDistributor<?> distributor() {
        return new GitHubAppDistributor( "<GitHub Access Token>", "repo", "owner",
                MAX_ASSETS, acceptVersion, selectDownloadAsset );
    }
}
```

`GitHubAppDistributor` avoids relying on anything other than the Java basic standard library
(module `java.base`) and a small JSON parser (modular version of [`org.json`](https://search.maven.org/artifact/com.guicedee.services/json))
by performing HTTP requests via `java.net.URLConnection` and building the GraphQL query (which is static) 
as a simple JSON String.

For this reason, it produces a tiny jar (20KB) and only adds a small dependency, `org.json` (72KB).
