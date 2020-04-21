package com.athaydes.keepup.github;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

final class Http
{
    static HttpURLConnection connect(URL url, String method)
            throws IOException
    {
        var con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod(method);
        con.setConnectTimeout(5000);
        con.setReadTimeout(5000);
        con.setInstanceFollowRedirects(true);
        return con;
    }

}
