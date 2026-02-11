package com.monteweb.shared;

import com.monteweb.shared.dto.ApiResponse;
import com.monteweb.shared.dto.ErrorResponse;
import com.monteweb.shared.dto.PageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ApiResponseTest {

    // ── ApiResponse ──────────────────────────────────────────────────

    @Test
    void ok_withData_shouldSetSuccessTrue() {
        var response = ApiResponse.ok("hello");

        assertTrue(response.success());
        assertEquals("hello", response.data());
        assertNull(response.message());
        assertNotNull(response.timestamp());
    }

    @Test
    void ok_withDataAndMessage_shouldSetBoth() {
        var response = ApiResponse.ok(42, "Found");

        assertTrue(response.success());
        assertEquals(42, response.data());
        assertEquals("Found", response.message());
    }

    @Test
    void ok_withNullData_shouldSucceed() {
        var response = ApiResponse.ok(null);

        assertTrue(response.success());
        assertNull(response.data());
    }

    @Test
    void error_shouldSetSuccessFalse() {
        var response = ApiResponse.error("Something went wrong");

        assertFalse(response.success());
        assertNull(response.data());
        assertEquals("Something went wrong", response.message());
        assertNotNull(response.timestamp());
    }

    // ── ErrorResponse ────────────────────────────────────────────────

    @Test
    void errorResponse_of_shouldCreateWithoutDetails() {
        var error = ErrorResponse.of("NOT_FOUND", "Resource not found", 404);

        assertEquals("NOT_FOUND", error.error());
        assertEquals("Resource not found", error.message());
        assertEquals(404, error.status());
        assertNull(error.details());
        assertNotNull(error.timestamp());
    }

    @Test
    void errorResponse_ofWithDetails_shouldIncludeDetails() {
        var details = Map.of("field", "must not be blank");
        var error = ErrorResponse.of("VALIDATION_ERROR", "Validation failed", 400, details);

        assertEquals("VALIDATION_ERROR", error.error());
        assertEquals(400, error.status());
        assertNotNull(error.details());
        assertEquals("must not be blank", error.details().get("field"));
    }

    // ── PageResponse ─────────────────────────────────────────────────

    @Test
    void pageResponse_from_shouldMapCorrectly() {
        var items = List.of("a", "b", "c");
        var page = new PageImpl<>(items, PageRequest.of(0, 10), 3);

        var response = PageResponse.from(page);

        assertEquals(3, response.content().size());
        assertEquals(0, response.page());
        assertEquals(10, response.size());
        assertEquals(3, response.totalElements());
        assertEquals(1, response.totalPages());
        assertTrue(response.last());
    }

    @Test
    void pageResponse_from_multiplePages_shouldReflectPagination() {
        var items = List.of("a", "b");
        var page = new PageImpl<>(items, PageRequest.of(0, 2), 5);

        var response = PageResponse.from(page);

        assertEquals(2, response.content().size());
        assertEquals(0, response.page());
        assertEquals(2, response.size());
        assertEquals(5, response.totalElements());
        assertEquals(3, response.totalPages());
        assertFalse(response.last());
    }

    @Test
    void pageResponse_emptyPage_shouldWork() {
        var page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        var response = PageResponse.from(page);

        assertTrue(response.content().isEmpty());
        assertEquals(0, response.totalElements());
        assertEquals(0, response.totalPages());
        assertTrue(response.last());
    }
}
