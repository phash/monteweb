package com.monteweb.shared;

import com.monteweb.shared.dto.ErrorResponse;
import com.monteweb.shared.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler(mock(ApplicationEventPublisher.class));
    }

    @Test
    void handleNotFound_shouldReturn404() {
        var ex = new ResourceNotFoundException("Item not found");
        ResponseEntity<ErrorResponse> response = handler.handleNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("NOT_FOUND");
        assertThat(response.getBody().message()).isEqualTo("Item not found");
    }

    @Test
    void handleBadRequest_shouldReturn400() {
        var ex = new BadRequestException("Invalid input");
        ResponseEntity<ErrorResponse> response = handler.handleBadRequest(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("BAD_REQUEST");
    }

    @Test
    void handleForbidden_shouldReturn403() {
        var ex = new ForbiddenException("Not allowed");
        ResponseEntity<ErrorResponse> response = handler.handleForbidden(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("FORBIDDEN");
    }

    @Test
    void handleBusiness_shouldReturn422() {
        var ex = new BusinessException("Business rule violated");
        ResponseEntity<ErrorResponse> response = handler.handleBusiness(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("BUSINESS_ERROR");
    }

    @Test
    void handleGeneric_shouldReturn500() {
        var ex = new RuntimeException("Something went wrong");
        ResponseEntity<ErrorResponse> response = handler.handleGeneric(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("INTERNAL_ERROR");
    }
}
