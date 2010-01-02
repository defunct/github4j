/**
 * 
 */
package com.goodworkalan.github.downloads;

import static com.goodworkalan.github.downloads.GitHubDownloadException.GITHUB_HTTP_ERROR;
import static com.goodworkalan.github.downloads.GitHubDownloadException.GITHUB_HTTP_FORBIDDEN;
import static com.goodworkalan.github.downloads.GitHubDownloadException.GITHUB_HTTP_IO;
import static com.goodworkalan.github.downloads.GitHubDownloadException.MALFORMED_URL;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;

public final class Download {
    /** The GitHub login. */
    private final String login;
    
    /** The GitHub API token. */
    private final String token;
    
    /** The project in the GitHub account. */
    private final String project;

    /** The download id. */
    private final String id;
    
    /** The download URL. */
    private final URL url;

    /**
     * Create a download record.
     * 
     * @param login
     *            The GitHub login.
     * @param project
     *            The project in the GitHub account.
     * @param token
     *            The GitHub API token.
     * @param id
     *            The download id.
     * @param url
     *            The download URL.
     */
    Download(String login, String token, String project, String id, URL url) {
        this.login = login;
        this.token = token;
        this.project = project;
        this.id = id;
        this.url = url;
    }
    
    /**
     * Get the download id.
     */
    public String getId() {
        return id;
    }
    
    /**
     * Get the download URL.
     * 
     * @return The download URL.
     */
    public URL getUrl() {
        return url;
    }
    
    public String getFileName() {
        URI base = URI.create("http://cloud.github.com/downloads/" + login + "/" + project + "/");
        return base.relativize(URI.create(url.toString())).toString();
    }
    
    /**
     * Delete the download.
     * 
     * @throws IOException For any I/O error.
     */
    public void delete() throws GitHubDownloadException {
        String apiCall = "http://github.com/" + login + "/" + project + "/downloads/" + id;
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(apiCall).openConnection();
            String data =
                "_method=delete&" +
                "login=" + URLEncoder.encode(login, "UTF-8") + "&" +
                "token=" + URLEncoder.encode(token, "UTF-8");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
            byte[] bytes = data.getBytes("UTF-8");
            connection.setRequestProperty("Content-Length", Integer.toString(bytes.length));
            connection.getOutputStream().write(bytes);
            connection.getOutputStream().flush();
            int responseCode = connection.getResponseCode();
            if (responseCode / 100 != 2) {
                if (responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                    throw new GitHubDownloadException(GITHUB_HTTP_FORBIDDEN, responseCode);
                }
                throw new GitHubDownloadException(GITHUB_HTTP_ERROR, responseCode);
            }
        } catch (UnsupportedEncodingException e) {
            // Never happens because UTF-8 is always supported.
            throw new RuntimeException(e);
        } catch (MalformedURLException e) {
            throw new GitHubDownloadException(MALFORMED_URL, apiCall);
        } catch (IOException e) {
            throw new GitHubDownloadException(GITHUB_HTTP_IO, apiCall);
        }
    }

    @Override
    public String toString() {
        return "{" + getId() + ", " + getUrl() + "}";
    }
}