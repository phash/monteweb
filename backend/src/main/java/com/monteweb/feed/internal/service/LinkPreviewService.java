package com.monteweb.feed.internal.service;

import com.monteweb.feed.LinkPreviewInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LinkPreviewService {

    private static final Logger log = LoggerFactory.getLogger(LinkPreviewService.class);

    private static final int MAX_CACHE_SIZE = 1000;
    private static final Duration CACHE_TTL = Duration.ofHours(1);
    private static final Duration FETCH_TIMEOUT = Duration.ofSeconds(3);

    // Regex patterns for OpenGraph meta tags
    private static final Pattern OG_TITLE = Pattern.compile(
            "<meta[^>]*property=[\"']og:title[\"'][^>]*content=[\"']([^\"']*)[\"']",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern OG_TITLE_ALT = Pattern.compile(
            "<meta[^>]*content=[\"']([^\"']*)[\"'][^>]*property=[\"']og:title[\"']",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern OG_DESC = Pattern.compile(
            "<meta[^>]*property=[\"']og:description[\"'][^>]*content=[\"']([^\"']*)[\"']",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern OG_DESC_ALT = Pattern.compile(
            "<meta[^>]*content=[\"']([^\"']*)[\"'][^>]*property=[\"']og:description[\"']",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern OG_IMAGE = Pattern.compile(
            "<meta[^>]*property=[\"']og:image[\"'][^>]*content=[\"']([^\"']*)[\"']",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern OG_IMAGE_ALT = Pattern.compile(
            "<meta[^>]*content=[\"']([^\"']*)[\"'][^>]*property=[\"']og:image[\"']",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern OG_SITE_NAME = Pattern.compile(
            "<meta[^>]*property=[\"']og:site_name[\"'][^>]*content=[\"']([^\"']*)[\"']",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern OG_SITE_NAME_ALT = Pattern.compile(
            "<meta[^>]*content=[\"']([^\"']*)[\"'][^>]*property=[\"']og:site_name[\"']",
            Pattern.CASE_INSENSITIVE);

    // Fallback: HTML <title> tag
    private static final Pattern HTML_TITLE = Pattern.compile(
            "<title[^>]*>([^<]*)</title>",
            Pattern.CASE_INSENSITIVE);
    // Fallback: meta description
    private static final Pattern META_DESC = Pattern.compile(
            "<meta[^>]*name=[\"']description[\"'][^>]*content=[\"']([^\"']*)[\"']",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern META_DESC_ALT = Pattern.compile(
            "<meta[^>]*content=[\"']([^\"']*)[\"'][^>]*name=[\"']description[\"']",
            Pattern.CASE_INSENSITIVE);

    private final HttpClient httpClient;
    private final ConcurrentHashMap<String, CachedPreview> cache = new ConcurrentHashMap<>();

    public LinkPreviewService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(FETCH_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public LinkPreviewInfo fetchPreview(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }

        // Check cache
        var cached = cache.get(url);
        if (cached != null && !cached.isExpired()) {
            return cached.preview;
        }

        try {
            var uri = URI.create(url);
            var scheme = uri.getScheme();
            if (scheme == null || (!scheme.equals("http") && !scheme.equals("https"))) {
                return null;
            }

            var request = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(FETCH_TIMEOUT)
                    .header("User-Agent", "MonteWeb/1.0 LinkPreview")
                    .header("Accept", "text/html")
                    .GET()
                    .build();

            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 400) {
                var html = response.body();
                var preview = parseOpenGraph(url, html);
                putCache(url, preview);
                return preview;
            }
        } catch (Exception e) {
            log.debug("Failed to fetch link preview for {}: {}", url, e.getMessage());
        }

        return null;
    }

    private LinkPreviewInfo parseOpenGraph(String url, String html) {
        if (html == null || html.isEmpty()) {
            return null;
        }

        // Limit parsing to the <head> section for performance
        String head = html;
        int headEnd = html.toLowerCase().indexOf("</head>");
        if (headEnd > 0) {
            head = html.substring(0, headEnd);
        }

        String title = extractFirst(head, OG_TITLE, OG_TITLE_ALT);
        String description = extractFirst(head, OG_DESC, OG_DESC_ALT);
        String imageUrl = extractFirst(head, OG_IMAGE, OG_IMAGE_ALT);
        String siteName = extractFirst(head, OG_SITE_NAME, OG_SITE_NAME_ALT);

        // Fallback to HTML title if no OG title
        if (title == null) {
            title = extractFirst(head, HTML_TITLE);
        }

        // Fallback to meta description if no OG description
        if (description == null) {
            description = extractFirst(head, META_DESC, META_DESC_ALT);
        }

        if (title == null && description == null && imageUrl == null) {
            return null;
        }

        // Decode HTML entities
        title = decodeHtmlEntities(title);
        description = decodeHtmlEntities(description);
        siteName = decodeHtmlEntities(siteName);

        return new LinkPreviewInfo(url, title, description, imageUrl, siteName);
    }

    private String extractFirst(String html, Pattern... patterns) {
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(html);
            if (matcher.find()) {
                String value = matcher.group(1);
                if (value != null && !value.isBlank()) {
                    return value.trim();
                }
            }
        }
        return null;
    }

    private String decodeHtmlEntities(String text) {
        if (text == null) return null;
        return text
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&apos;", "'");
    }

    private void putCache(String url, LinkPreviewInfo preview) {
        // Evict expired entries if cache is too large
        if (cache.size() >= MAX_CACHE_SIZE) {
            cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        }
        // If still too large, remove oldest entries
        if (cache.size() >= MAX_CACHE_SIZE) {
            var oldestKey = cache.entrySet().stream()
                    .min((a, b) -> a.getValue().createdAt.compareTo(b.getValue().createdAt))
                    .map(Map.Entry::getKey)
                    .orElse(null);
            if (oldestKey != null) {
                cache.remove(oldestKey);
            }
        }
        cache.put(url, new CachedPreview(preview, Instant.now()));
    }

    private record CachedPreview(LinkPreviewInfo preview, Instant createdAt) {
        boolean isExpired() {
            return Instant.now().isAfter(createdAt.plus(CACHE_TTL));
        }
    }
}
