package com.goodworkalan.github.downloads;
import static com.goodworkalan.github.downloads.GitHubDownloadException.*;
import static com.goodworkalan.github.downloads.GitHubDownloadException.GITHUB_HTTP_IO;
import static com.goodworkalan.github.downloads.GitHubDownloadException.GITHUB_HTTP_BAD_XML;
import static com.goodworkalan.github.downloads.GitHubDownloadException.GITHUB_HTTP_ERROR;
import static com.goodworkalan.github.downloads.GitHubDownloadException.GITHUB_HTTP_FORBIDDEN;
import static com.goodworkalan.github.downloads.GitHubDownloadException.MALFORMED_URL;
import static com.goodworkalan.github.downloads.GitHubDownloadException.S3_HTTP_IO;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class GitHubDownloads {
    private final static String ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";

    private final static String CRLF = "\r\n";

    private final String login;
    
    private final String token;
    
    public GitHubDownloads(String login, String token) {
        this.login = login;
        this.token = token;
    }
    
    public List<Download> getDownloads(String project) throws GitHubDownloadException {
        String apiCall = "http://github.com/" + login + "/" + project + "/downloads";
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
            Pattern pattern = Pattern.compile("id=\"download_(\\d+)\".*?href=\"(http://cloud.github.com/downloads/" + login + "/.*?)\"");
            StringBuilder buffer = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                buffer.append(line);
            }
            Matcher matcher = pattern.matcher(buffer);
            int index = 0;
            List<Download> downloads = new ArrayList<Download>();
            while (matcher.find(index)) {
                downloads.add(new Download(login, token, project, matcher.group(1), new URL(matcher.group(2))));
                index = matcher.end();
            }
            return downloads;
        } catch (MalformedURLException e) {
            throw new GitHubDownloadException(MALFORMED_URL, apiCall);
        } catch (IOException e) {
            throw new GitHubDownloadException(GITHUB_HTTP_IO, e);
        }
    }
    
    public void upload(String project, File file, String description, String contentType, String fileName) throws GitHubDownloadException {
        try {
            upload(project, new FileInputStream(file), file.length(), description, contentType, fileName);
        } catch (FileNotFoundException e) {
            throw new GitHubDownloadException(BODY_NOT_FOUND, e);
        }
    }

    public void upload(String project, InputStream body, long size, String description, String contentType, String fileName) throws GitHubDownloadException {
        Map<String, String> upload = new HashMap<String, String>();
        String apiCall = "http://github.com/" + login + "/" + project + "/downloads";
        try {
            URL url = new URL(apiCall);
            String data =
                "file_size=" + URLEncoder.encode(Long.toString(size), "UTF-8") + "&" +
                "content_type=" + URLEncoder.encode(contentType, "UTF-8") + "&" +
                "file_name=" + URLEncoder.encode(fileName, "UTF-8") + "&" +
                "description=" + URLEncoder.encode(description, "UTF-8") + "&" +
                "login=" + URLEncoder.encode(login, "UTF-8") + "&" +
                "token=" + URLEncoder.encode(token, "UTF-8");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            byte[] bytes = data.getBytes("UTF-8");
            connection.setRequestProperty("Content-Length", Integer.toString(bytes.length));
            connection.getOutputStream().write(bytes);
            connection.getOutputStream().flush();
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(connection.getInputStream());
            NodeList nodes = doc.getDocumentElement().getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    upload.put(element.getNodeName(), element.getTextContent());
                }
            }
        } catch (UnsupportedEncodingException e) {
            // Never happens because UTF-8 and ASCII are always supported.
            throw new RuntimeException(e);
        } catch (MalformedURLException e) {
            throw new GitHubDownloadException(MALFORMED_URL, apiCall).put("url", apiCall);
        } catch (IOException e) {
            throw new GitHubDownloadException(GITHUB_HTTP_IO, e);
        } catch (ParserConfigurationException e) {
            // Yeah, but we didn't do anything to change the configuration.
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new GitHubDownloadException(GITHUB_HTTP_BAD_XML, e);
        }
        try {
            Random random = new Random();
            StringBuilder newBoundary = new StringBuilder();
            for (int i = 0; i < 64; i++) {
                newBoundary.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
            }
            String boundary = newBoundary.toString();
            
            HttpURLConnection connection = (HttpURLConnection) new URL("http://github.s3.amazonaws.com/").openConnection();
            
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary.toString());
            
            OutputStream out = connection.getOutputStream();
            
            addField(out, boundary, "Filename", fileName);
            addField(out, boundary, "policy", upload.get("policy"));
            addField(out, boundary, "success_action_status", "201");
            addField(out, boundary, "key", upload.get("prefix") + fileName);
            addField(out, boundary, "AWSAccessKeyId", upload.get("accesskeyid"));
            addField(out, boundary, "Content-Type", contentType);
            addField(out, boundary, "signature", upload.get("signature"));
            addField(out, boundary, "acl", upload.get("acl"));
    
            StringBuilder field = new StringBuilder();
            field
                .append("--").append(boundary).append(CRLF)
                .append("Content-Disposition: form-data; name=\"file\"").append(CRLF)
                .append("Content-Type: ").append(contentType).append(CRLF).append(CRLF);
            out.write(field.toString().getBytes("ASCII"));
            
            try {
                byte[] buffer = new byte[4098];
                int read;
                while ((read = body.read(buffer)) != -1) {
                    try {
                        out.write(buffer, 0, read);
                    } catch (IOException e) {
                        throw new GitHubDownloadException(S3_HTTP_IO, e);
                    }
                }
            } catch (IOException e) {
                throw new GitHubDownloadException(BODY_IO, e);
            }
            
            field.setLength(0);
            field
                .append(CRLF)
                .append("--").append(boundary).append("--");
            out.write(field.toString().getBytes("ASCII"));
            out.flush();
            
            int responseCode = connection.getResponseCode();
            if (responseCode != 201) {
                if( responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                    throw new GitHubDownloadException(S3_HTTP_FORBIDDEN, responseCode);
                }
                throw new GitHubDownloadException(S3_HTTP_ERROR, responseCode);
            }
        } catch (UnsupportedEncodingException e) {
            // Never happens because UTF-8 and ASCII are always supported.
            throw new RuntimeException(e);
        } catch (MalformedURLException e) {
            // Never happens because the URL is a constant and obviosuly well-formed.
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new GitHubDownloadException(S3_HTTP_IO, e);
        }
    } 

    private void addField(OutputStream out, String boundary, String name, String value) throws UnsupportedEncodingException, IOException {
        StringBuilder field = new StringBuilder();
        field.append("--").append(boundary).append(CRLF)
            .append("Content-Disposition: form-data; name=\"").append(name).append("\"").append(CRLF).append(CRLF)
            .append(value).append(CRLF);
        out.write(field.toString().getBytes("ASCII"));
    }
}
