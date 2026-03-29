package com.buildmat.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves the React SPA's index.html for the root path and any non-API route,
 * enabling client-side routing to work correctly when users navigate directly
 * to a deep link or refresh the page.
 */
@RestController
public class SpaController {

    private static final ClassPathResource INDEX_HTML =
            new ClassPathResource("static/index.html");

    /** Serve index.html at the application root. */
    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<Resource> root() {
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(INDEX_HTML);
    }

    /**
     * Catch-all for any path that is not matched by a more specific mapping
     * (i.e. not an /api/** route or a static asset).  Returns index.html so
     * the React router can handle the path on the client side.
     *
     * Spring MVC resolves more-specific mappings first, so /api/** controllers
     * always win over this wildcard.
     */
    @GetMapping(value = "/**", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<Resource> spa() {
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(INDEX_HTML);
    }
}
