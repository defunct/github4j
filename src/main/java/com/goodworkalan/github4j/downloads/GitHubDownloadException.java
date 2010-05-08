package com.goodworkalan.github4j.downloads;

import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class GitHubDownloadException extends Exception {
    /** Serial version id. */
    private static final long serialVersionUID = 1L;

    /** The given login or project name resulted in a malformed URL. */
    public static final int MALFORMED_URL = 1001;
    
    /**  The HTTP request to delete a download raised an I/O exception. */
    public static final int GITHUB_HTTP_IO = 1101;
    
    /** The HTTP request to delete a download returned an error status. */ 
    public static final int GITHUB_HTTP_ERROR = 1102;

    /** The HTTP request github.com returned a forbidden error status. */ 
    public static final int GITHUB_HTTP_FORBIDDEN = 1103;
    
    /** The HTTP request github.com returned unparsable XML. */ 
    public static final int GITHUB_HTTP_BAD_XML = 1104;
    
    /** The HTTP request to amazon S3 an error status. */ 
    public static final int S3_HTTP_IO = 1201;
    
    /** The HTTP request to amazon S3 an error status. */ 
    public static final int S3_HTTP_ERROR = 1202;

    /** The HTTP request to amazon S3 a forbidden error status. */ 
    public static final int S3_HTTP_FORBIDDEN = 1203;

    /** Unable to read the upload content. */ 
    public static final int BODY_NOT_FOUND = 1401;
    
    /** Unable to read the upload content. */ 
    public static final int BODY_IO = 1402;

    /** The error code. */
    private final int code;
    
    /** The detail message format arguments. */
    private final Object[] arguments;
    
    /** Additional exception details. */
    private final Map<String, Object> properties = new HashMap<String, Object>();

    /**
     * Create a mix error with the given error code.
     * 
     * @param code
     *            The error code.
     */
    public GitHubDownloadException(int code, Object...arguments) {
        this.code = code;
        this.arguments = arguments;
    }

    /**
     * Create a mix error with the given error code.
     * 
     * @param code
     *            The error code.
     * @param cause
     *            The cause.
     */
    public GitHubDownloadException(int code, Throwable cause, Object...arguments) {
        super(null, cause);
        this.code = code;
        this.arguments = arguments;
    }
    
    public GitHubDownloadException put(String name, Object value) {
        properties.put(name, value);
        return this;
    }

    /**
     * Returns the detail message string of this error.
     * 
     * @return The detail message string of this error.
     */
    @Override
    public String getMessage() {
        ResourceBundle bundle = ResourceBundle.getBundle(getClass().getPackage().getName() + ".exceptions");
        String key = Integer.toString(code);
        try {
            return String.format(bundle.getString(key), arguments);
        } catch (MissingResourceException e) {
            return key;
        }
    }
}
