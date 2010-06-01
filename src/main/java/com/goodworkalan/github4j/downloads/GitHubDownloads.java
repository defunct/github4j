package com.goodworkalan.github4j.downloads;
import static com.goodworkalan.github4j.downloads.GitHubDownloadException.GITHUB_HTTP_ERROR;
import static com.goodworkalan.github4j.downloads.GitHubDownloadException.GITHUB_HTTP_FORBIDDEN;
import static com.goodworkalan.github4j.downloads.GitHubDownloadException.GITHUB_HTTP_IO;
import static com.goodworkalan.github4j.downloads.GitHubDownloadException.MALFORMED_URL;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// TODO Document.
public class GitHubDownloads {
    // TODO Document.
    public static final List<Download> getDownloads(String account, String project) throws GitHubDownloadException {
        String apiCall = "http://github.com/" + account + "/" + project + "/downloads";
        try {
            URL url = new URL(apiCall);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            int responseCode = connection.getResponseCode();
            if (responseCode / 100 != 2) {
                if( responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                    throw new GitHubDownloadException(GITHUB_HTTP_FORBIDDEN, responseCode);
                }
                throw new GitHubDownloadException(GITHUB_HTTP_ERROR, responseCode);
            }
            String encoding = "ISO-8859-1";
            String contentType = connection.getContentType();
            int encodingStart = contentType.indexOf("charset=");
            if (encodingStart != -1) {
                encoding = contentType.substring(encodingStart + "charset=".length());
            }
            InputStream in = new BufferedInputStream(connection.getInputStream());
            BufferedReader r = new BufferedReader(new InputStreamReader(in, encoding));
            Pattern pattern = Pattern.compile("id=\"download_(\\d+)\".*?href=\"(/downloads/" + account + "/" + project + "/.*?)\"");
            StringBuilder buffer = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                buffer.append(line);
            }
            Matcher matcher = pattern.matcher(buffer);
            int index = 0;
            List<Download> downloads = new ArrayList<Download>();
            while (matcher.find(index)) {
                downloads.add(new Download(account, project, matcher.group(1), new URL("http://github.com" + matcher.group(2))));
                index = matcher.end();
            }
            return downloads;
        } catch (MalformedURLException e) {
            throw new GitHubDownloadException(MALFORMED_URL, apiCall);
        } catch (IOException e) {
            throw new GitHubDownloadException(GITHUB_HTTP_IO, e);
        }
    }
}
