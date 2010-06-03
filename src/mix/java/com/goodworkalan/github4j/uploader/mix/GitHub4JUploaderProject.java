package com.goodworkalan.reflective.mix;

import com.goodworkalan.mix.ProjectModule;
import com.goodworkalan.mix.builder.Builder;
import com.goodworkalan.mix.builder.JavaProject;

/**
 * Builds the project definition for GitHub4J Uploader.
 *
 * @author Alan Gutierrez
 */
public class GitHub4JUploaderProject implements ProjectModule {
    /**
     * Build the project definition for GitHub4J Uploader.
     *
     * @param builder
     *          The project builder.
     */
    public void build(Builder builder) {
        builder
            .cookbook(JavaProject.class)
                .produces("com.github.bigeasy.github4j/github4j-uploader/0.1")
                .depends()
                    .production("org.testng/testng-jdk15/5.10")
                    .end()
                .end()
            .end();
    }
}
