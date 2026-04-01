package nto.web;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import nto.core.utils.exceptions.BadRequestException;
import nto.core.utils.exceptions.InvalidRefreshTokenException;
import nto.core.utils.exceptions.ResourceConflictException;
import nto.core.utils.exceptions.ServerBusyException;
import nto.web.advice.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;
    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
            .setControllerAdvice(exceptionHandler)
            .setValidator(validator)
            .build();
    }

    @Test
    void shouldReturnValidationErrorBodyForInvalidDto() throws Exception {
        mockMvc.perform(post("/test/validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value(ErrorMessages.VALID_ERROR.getMessage()))
            .andExpect(jsonPath("$.details.name").value("Name is required"))
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldReturnBadRequestForMalformedJson() throws Exception {
        mockMvc.perform(post("/test/validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Malformed JSON request"));
    }

    @Test
    void shouldReturnBadRequestForTypeMismatch() throws Exception {
        mockMvc.perform(get("/test/type/not-a-number"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error")
                .value("Invalid value for parameter 'id'. Expected Long"));
    }

    @Test
    void shouldReturnBadRequestForMissingRequestParameter() throws Exception {
        mockMvc.perform(get("/test/param"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Missing request parameter: value"));
    }

    @Test
    void shouldReturnBadRequestForMissingCookie() throws Exception {
        mockMvc.perform(get("/test/cookie"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Missing request cookie: sessionId"));
    }

    @Test
    void shouldReturnNotFoundForEntityNotFound() throws Exception {
        mockMvc.perform(get("/test/not-found"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Entity not found"));
    }

    @Test
    void shouldReturnForbiddenForAccessDenied() throws Exception {
        mockMvc.perform(get("/test/forbidden"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.error").value("Access denied"));
    }

    @Test
    void shouldReturnConflictForResourceConflict() throws Exception {
        mockMvc.perform(get("/test/conflict"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    void shouldReturnConflictForServerBusy() throws Exception {
        mockMvc.perform(get("/test/busy"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.error").value("Server is busy"));
    }

    @Test
    void shouldReturnUnauthorizedForInvalidRefreshToken() throws Exception {
        mockMvc.perform(get("/test/refresh-invalid"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.error").value("Refresh token is invalid"));
    }

    @Test
    void shouldReturnBadRequestForCustomBadRequestException() throws Exception {
        mockMvc.perform(get("/test/bad-request"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad input"));
    }

    @Test
    void shouldBuildNotFoundResponseForNoHandlerFoundException() {
        var response = exceptionHandler.handleNotFound(
            new NoHandlerFoundException("GET", "/missing", HttpHeaders.EMPTY));

        assertEquals(404, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(404, body.get("status"));
        assertEquals("No handler found for GET /missing", body.get("error"));
    }

    @RestController
    static class TestController {

        @PostMapping("/test/validation")
        Map<String, String> validate(@Valid @RequestBody SampleRequest request) {
            return Map.of("name", request.name());
        }

        @GetMapping("/test/type/{id}")
        Map<String, Long> typeMismatch(@PathVariable Long id) {
            return Map.of("id", id);
        }

        @GetMapping("/test/param")
        Map<String, String> missingParam(@RequestParam String value) {
            return Map.of("value", value);
        }

        @GetMapping("/test/cookie")
        Map<String, String> missingCookie(@CookieValue("sessionId") String sessionId) {
            return Map.of("sessionId", sessionId);
        }

        @GetMapping("/test/not-found")
        void entityNotFound() {
            throw new EntityNotFoundException("Entity not found");
        }

        @GetMapping("/test/forbidden")
        void forbidden() {
            throw new AccessDeniedException("Access denied");
        }

        @GetMapping("/test/conflict")
        void conflict() {
            throw new ResourceConflictException("Conflict");
        }

        @GetMapping("/test/busy")
        void busy() {
            throw new ServerBusyException("Server is busy");
        }

        @GetMapping("/test/refresh-invalid")
        void invalidRefresh() {
            throw new InvalidRefreshTokenException("Refresh token is invalid");
        }

        @GetMapping("/test/bad-request")
        void badRequest() {
            throw new BadRequestException("Bad input");
        }
    }

    record SampleRequest(@NotBlank(message = "Name is required") String name) {
    }
}
