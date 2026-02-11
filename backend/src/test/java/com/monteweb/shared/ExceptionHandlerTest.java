package com.monteweb.shared;

import com.monteweb.shared.exception.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for custom exception classes.
 */
class ExceptionHandlerTest {

    @Test
    void resourceNotFoundException_withNameAndId_shouldFormatMessage() {
        var ex = new ResourceNotFoundException("User", "abc-123");
        assertEquals("User not found with id: abc-123", ex.getMessage());
    }

    @Test
    void resourceNotFoundException_withMessage_shouldUseDirectly() {
        var ex = new ResourceNotFoundException("Custom message");
        assertEquals("Custom message", ex.getMessage());
    }

    @Test
    void badRequestException_shouldHaveMessage() {
        var ex = new BadRequestException("Invalid input");
        assertEquals("Invalid input", ex.getMessage());
    }

    @Test
    void businessException_shouldHaveMessage() {
        var ex = new BusinessException("Business rule violated");
        assertEquals("Business rule violated", ex.getMessage());
    }

    @Test
    void forbiddenException_withMessage_shouldHaveMessage() {
        var ex = new ForbiddenException("Not allowed here");
        assertEquals("Not allowed here", ex.getMessage());
    }

    @Test
    void forbiddenException_noArgs_shouldHaveDefaultMessage() {
        var ex = new ForbiddenException();
        assertEquals("Access denied", ex.getMessage());
    }

    @Test
    void allExceptions_shouldBeRuntimeExceptions() {
        assertInstanceOf(RuntimeException.class, new ResourceNotFoundException("test"));
        assertInstanceOf(RuntimeException.class, new BadRequestException("test"));
        assertInstanceOf(RuntimeException.class, new BusinessException("test"));
        assertInstanceOf(RuntimeException.class, new ForbiddenException("test"));
    }
}
