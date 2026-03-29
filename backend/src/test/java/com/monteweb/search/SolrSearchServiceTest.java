package com.monteweb.search;

import com.monteweb.search.internal.service.SolrSearchService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SolrSearchService.escapeQuery — validates that Solr
 * special characters are properly escaped to prevent query injection.
 *
 * <p>escapeQuery is private, so we access it via reflection. This avoids
 * needing a Spring context or Solr connection for a simple string utility.</p>
 */
class SolrSearchServiceTest {

    private static Method escapeQueryMethod;

    @BeforeAll
    static void setUp() throws Exception {
        escapeQueryMethod = SolrSearchService.class.getDeclaredMethod("escapeQuery", String.class);
        escapeQueryMethod.setAccessible(true);
    }

    private String escapeQuery(String input) throws Exception {
        // We need a SolrSearchService instance to invoke the method.
        // The constructor requires a SolrClient, but escapeQuery doesn't use it.
        // We pass null since escapeQuery is a pure string transformation.
        return (String) escapeQueryMethod.invoke(null, input);
    }

    /**
     * Try invoking as instance method (since it may not be static).
     * We create an instance via sun.misc.Unsafe or use a different approach.
     */
    private String callEscapeQuery(SolrSearchService instance, String input) throws Exception {
        return (String) escapeQueryMethod.invoke(instance, input);
    }

    @Test
    void escapeQuery_escapes_plus() throws Exception {
        var instance = createInstance();
        assertEquals("\\+", callEscapeQuery(instance, "+"));
    }

    @Test
    void escapeQuery_escapes_minus() throws Exception {
        var instance = createInstance();
        assertEquals("\\-", callEscapeQuery(instance, "-"));
    }

    @Test
    void escapeQuery_escapes_exclamation() throws Exception {
        var instance = createInstance();
        assertEquals("\\!", callEscapeQuery(instance, "!"));
    }

    @Test
    void escapeQuery_escapes_parentheses() throws Exception {
        var instance = createInstance();
        assertEquals("\\(\\)", callEscapeQuery(instance, "()"));
    }

    @Test
    void escapeQuery_escapes_curly_braces() throws Exception {
        var instance = createInstance();
        assertEquals("\\{\\}", callEscapeQuery(instance, "{}"));
    }

    @Test
    void escapeQuery_escapes_square_brackets() throws Exception {
        var instance = createInstance();
        assertEquals("\\[\\]", callEscapeQuery(instance, "[]"));
    }

    @Test
    void escapeQuery_escapes_caret() throws Exception {
        var instance = createInstance();
        assertEquals("\\^", callEscapeQuery(instance, "^"));
    }

    @Test
    void escapeQuery_escapes_tilde() throws Exception {
        var instance = createInstance();
        assertEquals("\\~", callEscapeQuery(instance, "~"));
    }

    @Test
    void escapeQuery_escapes_colon() throws Exception {
        var instance = createInstance();
        assertEquals("\\:", callEscapeQuery(instance, ":"));
    }

    @Test
    void escapeQuery_escapes_double_quote() throws Exception {
        var instance = createInstance();
        assertEquals("\\\"", callEscapeQuery(instance, "\""));
    }

    @Test
    void escapeQuery_escapes_asterisk() throws Exception {
        var instance = createInstance();
        assertEquals("\\*", callEscapeQuery(instance, "*"));
    }

    @Test
    void escapeQuery_escapes_question_mark() throws Exception {
        var instance = createInstance();
        assertEquals("\\?", callEscapeQuery(instance, "?"));
    }

    @Test
    void escapeQuery_escapes_forward_slash() throws Exception {
        var instance = createInstance();
        assertEquals("\\/", callEscapeQuery(instance, "/"));
    }

    @Test
    void escapeQuery_escapes_backslash() throws Exception {
        var instance = createInstance();
        assertEquals("\\\\", callEscapeQuery(instance, "\\"));
    }

    @Test
    void escapeQuery_escapes_all_special_characters() throws Exception {
        var instance = createInstance();
        String input = "\\+-!(){}[]^~:\"*?/";
        String expected = "\\\\\\+\\-\\!\\(\\)\\{\\}\\[\\]\\^\\~\\:\\\"\\*\\?\\/";
        assertEquals(expected, callEscapeQuery(instance, input));
    }

    @Test
    void escapeQuery_plain_text_unchanged() throws Exception {
        var instance = createInstance();
        assertEquals("hello world", callEscapeQuery(instance, "hello world"));
    }

    @Test
    void escapeQuery_alphanumeric_unchanged() throws Exception {
        var instance = createInstance();
        assertEquals("test123", callEscapeQuery(instance, "test123"));
    }

    @Test
    void escapeQuery_german_text_unchanged() throws Exception {
        var instance = createInstance();
        String german = "Klassenzimmer Putzaktion";
        assertEquals(german, callEscapeQuery(instance, german));
    }

    @Test
    void escapeQuery_mixed_text_and_specials() throws Exception {
        var instance = createInstance();
        // Simulates a user query like "foo:bar [test]"
        assertEquals("foo\\:bar \\[test\\]", callEscapeQuery(instance, "foo:bar [test]"));
    }

    @Test
    void escapeQuery_injection_attempt_is_escaped() throws Exception {
        var instance = createInstance();
        // Attempt to inject a Solr query like: *:* OR admin:true
        String malicious = "*:* OR admin:true";
        String escaped = callEscapeQuery(instance, malicious);
        assertTrue(escaped.contains("\\*"));
        assertTrue(escaped.contains("\\:"));
        assertFalse(escaped.contains("*:*")); // Raw injection pattern should not survive
    }

    /**
     * Create an instance of SolrSearchService without a real SolrClient.
     * escapeQuery is a pure string method that does not use solrClient,
     * so passing null is safe for these tests.
     */
    private SolrSearchService createInstance() {
        try {
            var constructor = SolrSearchService.class.getDeclaredConstructor(
                    org.apache.solr.client.solrj.SolrClient.class
            );
            constructor.setAccessible(true);
            return constructor.newInstance((org.apache.solr.client.solrj.SolrClient) null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create SolrSearchService instance for testing", e);
        }
    }
}
